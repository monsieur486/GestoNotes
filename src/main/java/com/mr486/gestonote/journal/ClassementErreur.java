package com.mr486.gestonote.journal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Range une exception remontée au conteneur web dans le type d'événement qui lui convient.
 *
 * <p><b>CLASSE COPIÉE, PAS UNE DÉPENDANCE.</b> Cette version est la référence, compilée et
 * testée ici ; les applications en portent une copie au paquet près, dont la révision est
 * tenue dans {@code client/README.md}. Une correction se fait ici d'abord.</p>
 *
 * <p>Un 404 n'est un défaut de l'application que si <b>l'une de ses propres pages</b> pointe
 * vers la ressource absente — ce que dit l'en-tête {@code Referer} quand il porte le même
 * hôte que la requête. Un robot arrive sans {@code Referer} ou avec un {@code Referer}
 * étranger ; l'outil de développement qui cherche un {@code .map} et le favori périmé aussi.
 * Le type d'exception, lui, ne désigne pas le robot : un vrai lien cassé et une sonde de
 * {@code /.env} lèvent la même {@link NoResourceFoundException}.</p>
 *
 * <p><b>Ne lève jamais</b> : l'observateur qui l'appelle ne doit pas tomber en observant.</p>
 */
public final class ClassementErreur {

    private static final String TYPE_INTERROMPUE = "appli.http.interrompue";
    private static final String TYPE_LIEN_CASSE = "appli.http.lien-casse";
    private static final String TYPE_INTROUVABLE = "appli.http.introuvable";
    private static final String TYPE_ERREUR = "appli.erreur";

    private static final String NIVEAU_INFO = "info";
    private static final String NIVEAU_AVERTISSEMENT = "avertissement";
    private static final String NIVEAU_ERREUR = "erreur";

    private ClassementErreur() {
    }

    /**
     * Classe une exception d'après sa nature et l'origine de la requête.
     *
     * <p><b>Exemple :</b> une {@code NoResourceFoundException} sur {@code /js/app.js} avec un
     * {@code Referer} {@code https://gestoturf.fr/partie/12} et un {@code Host}
     * {@code gestoturf.fr} rend {@code appli.http.lien-casse}, niveau {@code avertissement},
     * résumé « Lien cassé : /js/app.js demandé depuis /partie/12 ».</p>
     *
     * @param exception l'exception remontée, éventuellement {@code null}
     * @param chemin    le chemin demandé, tel que {@code getRequestURI()} le rend
     * @param referer   l'en-tête {@code Referer}, éventuellement {@code null}
     * @param hote      l'en-tête {@code Host}, éventuellement {@code null}
     * @return le type, le niveau, le résumé, et s'il faut joindre la trace
     */
    public static Classement classer(Exception exception, String chemin, String referer,
            String hote) {
        if (exception instanceof AsyncRequestNotUsableException) {
            return new Classement(TYPE_INTERROMPUE, NIVEAU_INFO,
                    "Réponse interrompue par le client sur " + chemin, false);
        }
        if (exception instanceof NoResourceFoundException) {
            String origine = cheminInterne(referer, hote);
            if (origine != null) {
                return new Classement(TYPE_LIEN_CASSE, NIVEAU_AVERTISSEMENT,
                        "Lien cassé : " + chemin + " demandé depuis " + origine, false);
            }
            return new Classement(TYPE_INTROUVABLE, NIVEAU_INFO,
                    "Ressource introuvable : " + chemin, false);
        }
        String nom = exception == null ? "Exception" : exception.getClass().getSimpleName();
        String message = exception == null ? null : exception.getMessage();
        return new Classement(TYPE_ERREUR, NIVEAU_ERREUR,
                nom + " sur " + chemin + " : " + message, true);
    }

    // Rend le chemin du Referer quand il désigne le même hôte que la requête, null sinon.
    // Le chemin seul : ni query string ni fragment, qui peuvent porter un jeton et seraient
    // gardés un mois dans le résumé. getRawPath() et non getPath(), pour rester dans le même
    // encodage que getRequestURI().
    private static String cheminInterne(String referer, String hote) {
        URI uri = uriDe(referer);
        if (uri == null || uri.getHost() == null || hote == null || hote.isBlank()
                || !memeHote(uri.getHost(), hote)) {
            return null;
        }
        String chemin = uri.getRawPath();
        return chemin == null || chemin.isEmpty() ? "/" : chemin;
    }

    // Lit le Referer comme une URI ; null s'il est absent, vide ou malformé.
    private static URI uriDe(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            return new URI(referer.trim());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    // Compare deux noms d'hôte sans leur port ni leur casse. L'égalité est stricte :
    // « www.gestoturf.fr » n'est pas « gestoturf.fr ». Les adresses IPv6 entre crochets ne
    // sont pas traitées — aucun site du parc n'est servi sous une IP littérale.
    private static boolean memeHote(String hoteReferer, String hote) {
        String sansPort = hote.trim();
        int deuxPoints = sansPort.indexOf(':');
        if (deuxPoints >= 0) {
            sansPort = sansPort.substring(0, deuxPoints);
        }
        return hoteReferer.toLowerCase(Locale.ROOT).equals(sansPort.toLowerCase(Locale.ROOT));
    }

    /**
     * Ce que l'observateur doit émettre.
     *
     * <p><b>Exemple :</b> {@code new Classement("appli.http.introuvable", "info",
     * "Ressource introuvable : /.env", false)}.</p>
     *
     * @param type      type hiérarchique de l'événement
     * @param niveau    {@code info}, {@code avertissement} ou {@code erreur}
     * @param resume    la phrase que le flux affichera
     * @param avecTrace vrai s'il faut joindre la pile d'appels en détails
     */
    public record Classement(String type, String niveau, String resume, boolean avecTrace) {
    }
}
