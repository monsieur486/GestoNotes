package com.mr486.gestonote.journal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Branche l'application au journal central du parc.
 *
 * <p>Deux faits sont consignés, et eux seuls : le démarrage, et les exceptions qui remontent
 * jusqu'au conteneur web. <b>Aucun appender SLF4J n'est branché sur le logger racine</b> : il
 * remonterait tout {@code WARN}, noierait le journal au premier composant bavard, et le quota
 * horaire se contenterait alors de refuser en {@code 429} — le vrai problème serait masqué au
 * lieu d'être montré.</p>
 *
 * <p><b>L'émission ne peut pas nuire à l'application.</b> Le client ne lance jamais, ne bloque
 * jamais, et reste inerte si l'adresse ou le jeton manquent.</p>
 */
@Configuration
public class JournalConfiguration {

    /** Nom du composant tel qu'il apparaîtra dans le journal. */
    private static final String COMPOSANT = "gestonotes";

    /**
     * Déclare le client d'émission.
     *
     * <p>{@code destroyMethod} plutôt qu'un {@code @PreDestroy} dans la classe : le client est
     * fait pour être copié tel quel dans plusieurs applications, et ne porte donc aucune
     * annotation Spring.</p>
     *
     * @param url   l'adresse d'ingestion du journal, vide si non configurée
     * @param jeton le jeton porteur de la machine, vide si non configuré
     * @return le client, inerte si l'un des deux manque
     */
    @Bean(destroyMethod = "fermer")
    public JournalClient journalClient(
            @Value("${configuration.journal.url:}") String url,
            @Value("${configuration.journal.jeton:}") String jeton) {
        return new JournalClient(url, jeton, COMPOSANT);
    }

    /**
     * Signale le démarrage, une fois l'application réellement prête à servir.
     *
     * <p>{@link ApplicationReadyEvent} et non le constructeur : un événement émis pendant
     * l'assemblage du contexte annoncerait un démarrage qui peut encore échouer.</p>
     *
     * @param client le client d'émission
     * @return l'écouteur, déclaré en bean pour rester visible dans le contexte
     */
    @Bean
    public DemarrageSignale demarrageSignale(JournalClient client) {
        return new DemarrageSignale(client);
    }

    /** Porte l'écoute du démarrage, pour garder la configuration lisible. */
    public record DemarrageSignale(JournalClient client) {

        /**
         * Émet {@code appli.demarrage} lorsque l'application est prête.
         *
         * <p><b>Exemple :</b> après un redémarrage de la VM, cet événement date le retour du
         * service à la seconde près.</p>
         *
         * @param evenement l'événement Spring de démarrage terminé
         */
        @EventListener(ApplicationReadyEvent.class)
        public void signaler(ApplicationReadyEvent evenement) {
            client.emettre("appli.demarrage", "info", "GestoNotes démarré");
        }
    }
}
