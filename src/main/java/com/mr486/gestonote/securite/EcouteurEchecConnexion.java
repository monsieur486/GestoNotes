package com.mr486.gestonote.securite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * Consigne chaque échec d'authentification, avec l'adresse de son auteur.
 *
 * <p>Sans cette trace, un mot de passe refusé est <b>invisible de l'extérieur</b> : le formulaire
 * de connexion rend {@code 200} comme une connexion réussie, si bien qu'un proxy ne peut pas
 * distinguer l'échec du succès. Rien ne permettait donc de ralentir un devinage, alors que la
 * machine sert cinq sites depuis internet.</p>
 *
 * <p>La ligne produite est destinée à être lue par fail2ban : <b>son format est un contrat</b>, et
 * non une commodité de lecture. La modifier sans modifier le filtre correspondant laisserait une
 * protection qui ne bannit plus rien, sans qu'aucune erreur ne le signale. Voir
 * {@code infra/web/fail2ban/} dans le dépôt d'infrastructure.</p>
 */
@Component
public class EcouteurEchecConnexion {

    /** Adresse rendue quand l'échec ne vient pas d'une requête HTTP (tâche, appel interne). */
    private static final String ADRESSE_INCONNUE = "inconnue";

    /**
     * Journal dédié, et non celui de la classe : c'est ce nom que la configuration logback dirige
     * vers le fichier lu par fail2ban. Le lier au nom de la classe rendrait ce fichier tributaire
     * d'un renommage ou d'un déplacement de paquetage.
     */
    private static final Logger JOURNAL = LoggerFactory.getLogger("securite.connexion");

    /**
     * Consigne un échec d'authentification survenu sur l'application.
     *
     * <p><b>Exemple :</b> une tentative de {@code toto} depuis {@code 203.0.113.7} produit la ligne
     * {@code ECHEC_CONNEXION identifiant="toto" adresse=203.0.113.7}.</p>
     *
     * @param evenement l'événement d'échec publié par Spring Security
     */
    @EventListener
    public void consigner(AbstractAuthenticationFailureEvent evenement) {
        Authentication tentative = evenement.getAuthentication();
        // Le mot de passe présenté vit dans les « credentials » de la tentative : il n'est jamais
        // lu ici, et ne doit jamais l'être — un journal lu par fail2ban est un fichier qui traîne.
        JOURNAL.warn("ECHEC_CONNEXION identifiant=\"{}\" adresse={}",
                tentative.getName(), adresseDe(tentative));
    }

    // L'adresse ne vient pas de l'événement mais des « détails » de la tentative, que Spring
    // Security remplit avec la requête HTTP. Un échec hors requête n'en a pas.
    private String adresseDe(Authentication tentative) {
        if (tentative.getDetails() instanceof WebAuthenticationDetails details) {
            String adresse = details.getRemoteAddress();
            return adresse == null || adresse.isBlank() ? ADRESSE_INCONNUE : adresse;
        }
        return ADRESSE_INCONNUE;
    }
}
