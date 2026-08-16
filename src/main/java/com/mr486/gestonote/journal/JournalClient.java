package com.mr486.gestonote.journal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Le client d'émission des applications — et la seule propriété qui compte : il ne peut pas
 * nuire à l'application qui l'héberge.
 *
 * <p><b>Cette classe est faite pour être COPIÉE</b>, pas pour être dépendue. La version de
 * référence vit ici ; {@code client/README.md} porte la procédure de copie, le numéro de
 * révision et la liste des applications qui l'embarquent. Elle tient volontairement en un seul
 * fichier, et n'a d'autre dépendance que SLF4J — présent dans toute application Java du
 * parc, quelle que soit sa version de Spring Boot.</p>
 *
 * <p><b>Trois décisions techniques, prises sur des faits mesurés et non sur des
 * préférences :</b></p>
 *
 * <ol>
 *   <li><b>{@link HttpURLConnection} et jamais {@code java.net.http}.</b> Le 2026-08-08,
 *   {@code java.net.http} a été mesuré sur {@code gt-import} à 21 succès pour 19 échecs, soit
 *   53 %, contre 89 % avec {@code HttpURLConnection} sur la fenêtre suivante — avec la
 *   signature {@code HTTP/1.1 header parser received no bytes}, celle d'une connexion
 *   persistante morte réutilisée. {@code gt-import} est l'une des cinq applications
 *   instrumentées, et elle tourne derrière le tunnel gluetun où cela s'est produit. De plus,
 *   {@code -Dhttp.keepAlive=false}, posé dans son Compose versionné, <b>ne s'applique pas</b>
 *   à {@code java.net.http}, qui a son propre pool. L'argumentaire théorique en faveur de
 *   {@code java.net.http} a déjà été fait une fois, et il était faux : ne pas le refaire sans
 *   mesurer.</li>
 *   <li><b>Aucune bibliothèque JSON, et c'est un revirement assumé.</b> La conception prévoyait
 *   Jackson, « présent dans toute application Spring Boot Web ». C'est faux ici : Spring Boot 4
 *   est passé à Jackson 3, dont le paquet est {@code tools.jackson.databind} là où Boot 3
 *   employait {@code com.fasterxml.jackson.databind}. Une classe destinée à être copiée dans
 *   cinq applications ne peut pas porter un import qui dépend de leur version de Boot — elle
 *   cesserait de compiler chez la première qui n'aurait pas migré. Le message rendu ici est un
 *   objet plat de huit chaînes : l'écrire à la main coûte un échappement, et
 *   {@code JournalClientTest} le garde en reparsant le JSON produit avec un vrai analyseur.
 *   Le seul piège réel — apostrophes, guillemets et sauts de ligne d'un résumé — est
 *   exactement ce que ce test vérifie.</li>
 *   <li><b>Une classe copiée, pas un artefact Maven.</b> Décision de conception : cinq
 *   applications aux cycles de construction indépendants n'ont pas à partager une dépendance
 *   qu'il faudrait publier, versionner et faire monter partout à chaque correctif.</li>
 * </ol>
 *
 * <p><b>Exemple :</b>
 * {@code JournalClient client = new JournalClient(url, jeton, "gestoturf");}
 * puis {@code client.emettre("appli.demarrage", "info", "Application démarrée");}</p>
 */
// PMD signale ici deux choses, et l'on choisit de les taire EN CONNAISSANCE DE CAUSE plutôt
// que de les contourner :
//
//   TooManyMethods — la classe ferait un découpage naturel (sérialisation d'un côté, file
//   d'envoi de l'autre). Mais elle « tient volontairement en un seul fichier » pour être COPIÉE
//   dans cinq applications aux cycles de construction indépendants ; la scinder ferait deux
//   fichiers à recopier, à versionner et à garder en phase. Le compteur perdrait ce que la
//   copie gagnerait.
//
//   CyclomaticComplexity — elle vise « echapper », dont la complexité EST la spécification :
//   sept caractères JSON à échapper, plus les non imprimables. La découper en sous-méthodes
//   déplacerait les branches sans rien clarifier.
//
// Les deux sont des conflits avec une décision de conception documentée, pas des défauts.
//   GodClass — même conflit, vu par une autre règle : le nombre de méthodes, de champs et
//   de branches d'une classe qui fait tout ce qu'il faut pour ne rien coûter à son hôte.
//
//   TropDeCollaborateursInjectes — faux positif : cette classe n'injecte RIEN. Elle n'a pas de
//   constructeur Spring, ses champs sont sa configuration et son état interne, et c'est
//   précisément ce qui lui permet d'être copiée sans dépendre d'un conteneur.
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CyclomaticComplexity", "PMD.GodClass",
                   "PMD.TropDeCollaborateursInjectes"})
public class JournalClient implements AutoCloseable {

    /** Nombre d'événements gardés en attente. Au-delà, le plus ancien est écarté. */
    public static final int TAILLE_FILE = 500;

    /** Nombre d'événements envoyés en un seul appel. */
    private static final int TAILLE_LOT = 100;

    private static final int DELAI_CONNEXION_MS = 3_000;
    private static final int DELAI_LECTURE_MS = 5_000;

    /** Attente du fil d'envoi entre deux tours, quand la file est vide. */
    private static final long ATTENTE_FILE_MS = 500;

    /** Délai laissé au fil d'envoi pour finir son tour à la fermeture. */
    private static final long ATTENTE_ARRET_MS = 2_000;

    /** Premier code HTTP au-delà duquel la réponse n'est plus un succès. */
    private static final int PREMIER_CODE_ECHEC = 300;

    /** Type émis à la reprise pour dire ce qui a été perdu pendant la coupure. */
    private static final String TYPE_PERTES = "journal.client.pertes";

    /** Premier caractère imprimable : en deçà, l'échappement passe en {@code \\u00xx}. */
    private static final char PREMIER_IMPRIMABLE = ' ';

    private static final Logger LOG = LoggerFactory.getLogger(JournalClient.class);

    private final String url;
    private final String jeton;
    private final String composant;
    /**
     * Déclarée par son CONTRAT et non par sa classe : rien ici n'a besoin de savoir que la file
     * est bornée par un tableau, et le jour où une autre implémentation conviendrait mieux, seule
     * cette ligne changerait.
     */
    private final BlockingQueue<EvenementSortant> file =
            new ArrayBlockingQueue<>(TAILLE_FILE);
    private final AtomicLong compteurPertes = new AtomicLong();
    private final Thread fil;

    /**
     * Faux quand l'adresse ou le jeton manquent : le client est alors INERTE.
     *
     * <p>Une application lancée sans configuration de journal — sur un poste de développement,
     * dans une suite de tests, dans un conteneur dont le {@code .env} n'a pas encore été
     * renseigné — doit démarrer exactement comme avant. Sans cette garde, elle porterait un fil
     * qui tenterait une connexion vers une adresse vide toutes les demi-secondes, indéfiniment.
     * L'instrumentation ne doit rien coûter à qui ne l'a pas configurée.</p>
     */
    private final boolean configure;

    private volatile boolean actif = true;

    /**
     * Construit le client et démarre son fil d'envoi.
     *
     * <p>Le fil est un démon : il n'empêche jamais l'arrêt de la machine virtuelle, quelle
     * que soit la file restante. Un journal indisponible ne doit pas retenir une application
     * qui s'arrête.</p>
     *
     * <p><b>Exemple :</b> {@code new JournalClient(System.getenv("JOURNAL_URL"),
     * System.getenv("JOURNAL_JETON"), "gt-import")}</p>
     *
     * @param url       l'adresse complète de l'ingestion unitaire, dont le lot est déduit
     * @param jeton     le jeton porteur de la machine
     * @param composant le nom du composant émetteur
     */
    public JournalClient(String url, String jeton, String composant) {
        this.url = url;
        this.jeton = jeton;
        this.composant = composant;
        this.configure = adresseUtilisable(url) && jeton != null && !jeton.isBlank();

        if (!configure) {
            LOG.info("Journal central non configuré : émission désactivée.");
            this.fil = null;
            return;
        }
        this.fil = new Thread(this::boucler, "journal-client");
        this.fil.setDaemon(true);
        this.fil.start();
    }

    // L'adresse vient de la configuration, jamais d'un utilisateur — mais rien ne la validait.
    // « file:///etc/passwd » ou « jar:... » auraient été ouverts sans un mot, et une faute de
    // frappe dans un .env aurait produit un fil tentant indéfiniment une adresse absurde.
    //
    // Une adresse refusée rend le client INERTE, exactement comme une configuration vide : elle
    // ne lève pas. L'instrumentation ne doit jamais empêcher une application de démarrer, et
    // c'est la propriété qui gouverne toute cette classe.
    private static boolean adresseUtilisable(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            String schema = URI.create(url.trim()).getScheme();
            boolean acceptee = "http".equalsIgnoreCase(schema) || "https".equalsIgnoreCase(schema);
            if (!acceptee) {
                LOG.warn("Journal central : adresse ignorée, schéma « {} » non accepté.", schema);
            }
            return acceptee;
        } catch (IllegalArgumentException adresseIllisible) {
            LOG.warn("Journal central : adresse illisible, émission désactivée.");
            return false;
        }
    }

    /**
     * Sérialise un événement en JSON.
     *
     * <p>Publique et statique pour être éprouvée seule : c'est le point où un résumé porteur
     * d'apostrophes, de guillemets ou de sauts de ligne se perdrait silencieusement.</p>
     *
     * <p><b>Exemple :</b> un résumé valant {@code L'« erreur » a dit : "non"} traverse la
     * sérialisation sans perte.</p>
     *
     * @param evenement l'événement à sérialiser
     * @return sa représentation JSON
     */
    public static String serialiser(EvenementSortant evenement) {
        StringBuilder json = new StringBuilder().append('{');
        champ(json, "composant", evenement.composant(), true);
        champ(json, "type", evenement.type(), false);
        champ(json, "niveau", evenement.niveau(), false);
        champ(json, "resume", evenement.resume(), false);
        champ(json, "details", evenement.details(), false);
        champ(json, "operation", evenement.operation(), false);
        champ(json, "cleEmetteur", evenement.cleEmetteur(), false);
        champ(json, "horodatage", evenement.horodatage(), false);
        return json.append('}').toString();
    }

    /**
     * Échappe une chaîne pour l'insérer dans du JSON.
     *
     * <p>Publique et statique pour être éprouvée seule : c'est le seul endroit où un résumé
     * pourrait se perdre.</p>
     *
     * <p><b>Exemple :</b> {@code L'« erreur » a dit : "non"} devient
     * {@code L'« erreur » a dit : \"non\"}. Les caractères accentués ne sont pas échappés :
     * le corps part en UTF-8, déclaré comme tel dans l'en-tête de type.</p>
     *
     * @param valeur la chaîne à échapper
     * @return la chaîne échappée, sans ses guillemets englobants
     */
    public static String echapper(String valeur) {
        StringBuilder sortie = new StringBuilder();
        for (int i = 0; i < valeur.length(); i++) {
            char caractere = valeur.charAt(i);
            switch (caractere) {
                case '"' -> sortie.append("\\\"");
                case '\\' -> sortie.append("\\\\");
                case '\n' -> sortie.append("\\n");
                case '\r' -> sortie.append("\\r");
                case '\t' -> sortie.append("\\t");
                case '\b' -> sortie.append("\\b");
                case '\f' -> sortie.append("\\f");
                default -> {
                    if (caractere < PREMIER_IMPRIMABLE) {
                        sortie.append(String.format("\\u%04x", (int) caractere));
                    } else {
                        sortie.append(caractere);
                    }
                }
            }
        }
        return sortie.toString();
    }

    // Un champ du message, ou « null » littéral s'il est absent — le serveur traite les deux
    // de la même façon, et omettre le champ ferait diverger le message du contrat déclaré.
    private static void champ(StringBuilder json, String nom, String valeur, boolean premier) {
        if (!premier) {
            json.append(',');
        }
        json.append('"').append(nom).append("\":");
        if (valeur == null) {
            json.append("null");
        } else {
            json.append('"').append(echapper(valeur)).append('"');
        }
    }

    // Le lot, tel qu'il part sur le fil : un tableau JSON d'événements.
    private static String serialiserLot(List<EvenementSortant> lot) {
        StringBuilder json = new StringBuilder().append('[');
        for (int i = 0; i < lot.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(serialiser(lot.get(i)));
        }
        return json.append(']').toString();
    }

    /**
     * Émet un événement simple.
     *
     * <p><b>Cette méthode ne lance jamais et ne bloque jamais</b>, quel que soit l'état du
     * journal, du réseau ou de la file. Le pire cas est la perte de l'événement — jamais un
     * import interrompu ni une requête HTTP en échec.</p>
     *
     * <p><b>Exemple :</b> {@code emettre("appli.import.termine", "info", "1 284 cotes")}</p>
     *
     * @param type   le type hiérarchique, au moins deux segments
     * @param niveau {@code info}, {@code avertissement}, {@code erreur} ou {@code critique}
     * @param resume une phrase courte
     */
    public void emettre(String type, String niveau, String resume) {
        emettre(new EvenementSortant(composant, type, niveau, resume, null, null, null,
                Instant.now().toString()));
    }

    /**
     * Émet un événement complet.
     *
     * <p>La file est bornée et <b>n'est jamais bloquante</b> : pleine, elle écarte le plus
     * ancien pour faire place au plus récent, et compte la perte. Un {@code put} bloquant
     * figerait l'appelant, ce qui serait la pire façon de tomber — l'application cesserait de
     * fonctionner à cause de son journal.</p>
     *
     * <p><b>Exemple :</b> une application partie en boucle d'erreur remplit la file en
     * quelques secondes ; elle continue de tourner, et la reprise dira combien d'événements
     * ont été écartés.</p>
     *
     * @param evenement l'événement à émettre
     */
    public void emettre(EvenementSortant evenement) {
        if (!configure) {
            return;
        }
        if (!file.offer(evenement)) {
            file.poll();
            compteurPertes.incrementAndGet();
            if (!file.offer(evenement)) {
                compteurPertes.incrementAndGet();
            }
        }
    }

    /**
     * Rend le nombre d'événements en attente d'envoi.
     *
     * <p><b>Exemple :</b> sert aux tests à vérifier que la file reste bornée, et à une sonde
     * applicative à constater qu'elle ne se vide plus.</p>
     *
     * @return la taille courante de la file
     */
    public int tailleFile() {
        return file.size();
    }

    /**
     * Rend le nombre d'événements écartés depuis la dernière reprise.
     *
     * @return le compteur de pertes
     */
    public long pertes() {
        return compteurPertes.get();
    }

    /**
     * Vide la file une dernière fois et arrête le fil d'envoi.
     *
     * <p>À appeler depuis un {@code @PreDestroy} côté application. Sans cela, les derniers
     * événements — dont celui qui dirait pourquoi l'application s'arrête — resteraient dans
     * la file.</p>
     *
     * <p><b>Exemple :</b> l'arrêt d'un conteneur laisse au client le temps d'un dernier
     * envoi, borné à quelques secondes.</p>
     */
    public void fermer() {
        if (!configure) {
            return;
        }
        actif = false;
        envoyerUnLot();
        fil.interrupt();
        try {
            fil.join(ATTENTE_ARRET_MS);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
        }
    }

    /** Alias de {@link #fermer()}, pour un emploi en {@code try}-avec-ressources. */
    @Override
    public void close() {
        fermer();
    }

    // Le fil d'envoi. Il ne meurt jamais d'une exception : une erreur non rattrapée le
    // tuerait, et l'application émettrait alors dans une file que plus personne ne vide —
    // une panne parfaitement silencieuse.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void boucler() {
        while (actif) {
            try {
                envoyerUnLot();
                Thread.sleep(ATTENTE_FILE_MS);
            } catch (InterruptedException interruption) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException erreur) {
                LOG.debug("Fil d'envoi du journal : {}", erreur.toString());
            }
        }
    }

    // Prend ce qui attend, l'envoie, et signale à la reprise ce qui a été perdu. Un échec
    // perd le lot : le client n'a pas de tampon sur disque, contrairement aux scripts
    // d'infrastructure. C'est assumé — une application n'a pas à écrire hors de sa mémoire
    // pour consigner un fait secondaire.
    private void envoyerUnLot() {
        List<EvenementSortant> lot = new ArrayList<>();
        file.drainTo(lot, TAILLE_LOT);
        if (lot.isEmpty()) {
            return;
        }
        if (!poster(lot)) {
            compteurPertes.addAndGet(lot.size());
            return;
        }
        long perdus = compteurPertes.getAndSet(0);
        if (perdus > 0) {
            // Sans cet événement, un incident de connectivité effacerait sa propre trace :
            // le journal ne saurait jamais qu'il lui manque quelque chose.
            poster(List.of(new EvenementSortant(composant, TYPE_PERTES, "avertissement",
                    perdus + " événement(s) écarté(s) faute de pouvoir joindre le journal",
                    null, null, null, Instant.now().toString())));
        }
    }

    // Un envoi. Rend vrai si le journal a accepté ; toute erreur est journalisée en debug et
    // jamais propagée — un journal indisponible ne doit pas remplir les logs de
    // l'application, ce qui serait une seconde nuisance par-dessus la première.
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.CloseResource"})
    private boolean poster(List<EvenementSortant> lot) {
        HttpURLConnection connexion = null;
        try {
            connexion = (HttpURLConnection) URI.create(urlDuLot()).toURL().openConnection();
            connexion.setRequestMethod("POST");
            connexion.setConnectTimeout(DELAI_CONNEXION_MS);
            connexion.setReadTimeout(DELAI_LECTURE_MS);
            connexion.setRequestProperty("Authorization", "Bearer " + jeton);
            connexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            // La connexion n'est pas réutilisée, et c'est le cœur du correctif du
            // 2026-08-08 : la panne mesurée était une connexion persistante morte reprise
            // dans le pool, derrière un tunnel qui la coupe sans le dire.
            connexion.setRequestProperty("Connection", "close");
            connexion.setDoOutput(true);

            byte[] corps = serialiserLot(lot).getBytes(StandardCharsets.UTF_8);
            try (OutputStream sortie = connexion.getOutputStream()) {
                sortie.write(corps);
            }
            int code = connexion.getResponseCode();
            if (code >= PREMIER_CODE_ECHEC) {
                LOG.debug("Journal : réponse {} pour {} événement(s)", code, lot.size());
                return false;
            }
            return true;
        } catch (IOException | RuntimeException erreur) {
            LOG.debug("Journal injoignable : {}", erreur.toString());
            return false;
        } finally {
            if (connexion != null) {
                connexion.disconnect();
            }
        }
    }

    // L'URL configurée désigne l'ingestion unitaire ; le lot est à côté. Déduire évite une
    // seconde variable d'environnement à tenir juste dans cinq applications.
    private String urlDuLot() {
        return url.endsWith("/lot") ? url : url + "/lot";
    }

    /**
     * Ce qu'une application envoie au journal.
     *
     * <p><b>La machine n'y figure pas</b> : elle est déduite du jeton par le serveur. Un champ
     * absent du contrat rend la falsification impossible plutôt qu'improbable.</p>
     *
     * <p><b>L'horodatage est posé par l'émetteur</b> et transmis en texte ISO-8601. Un
     * événement resté en file quelques minutes serait sinon daté de sa livraison, et l'on
     * perdrait l'instant de la panne qui l'y a mis. Le texte plutôt qu'un {@code Instant}
     * évite au client de dépendre d'un module Jackson optionnel dans cinq applications.</p>
     *
     * @param composant   qui parle sur la machine
     * @param type        type hiérarchique à points, au moins deux segments
     * @param niveau      {@code info}, {@code avertissement}, {@code erreur} ou {@code critique}
     * @param resume      une phrase courte
     * @param details     trace ou sortie de commande, tronquée par le serveur au-delà de 16 Ko
     * @param operation   relie les événements d'une même opération
     * @param cleEmetteur clé d'idempotence, facultative
     * @param horodatage  l'instant du fait, au format ISO-8601
     */
    public record EvenementSortant(
            String composant,
            String type,
            String niveau,
            String resume,
            String details,
            String operation,
            String cleEmetteur,
            String horodatage) {
    }
}
