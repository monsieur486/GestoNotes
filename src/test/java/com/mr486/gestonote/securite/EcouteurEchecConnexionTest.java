package com.mr486.gestonote.securite;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * La ligne produite ici est un CONTRAT avec fail2ban, pas un confort de lecture : son format est
 * repris tel quel dans le filtre {@code gt-ui-auth} du dépôt d'infrastructure. La changer sans
 * changer le filtre laisserait une protection qui ne bannit plus rien, sans qu'aucune erreur ne
 * le signale — c'est exactement ce qu'on cherche à éviter en la journalisant.
 */
// 203.0.113.0/24 est la plage RÉSERVÉE À LA DOCUMENTATION (RFC 5737) : une adresse en dur
// est ici exactement ce qu'il faut, et PMD la signale à tort. Employer une vraie adresse
// serait le vrai défaut — un test ne doit désigner aucune machine réelle.
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class EcouteurEchecConnexionTest {

    private static final String LOGGER = "securite.connexion";

    private ListAppender<ILoggingEvent> lignes;
    private Logger logger;

    @BeforeEach
    void brancherLeCollecteur() {
        lignes = new ListAppender<>();
        lignes.start();
        logger = (Logger) LoggerFactory.getLogger(LOGGER);
        logger.addAppender(lignes);
    }

    @AfterEach
    void debrancher() {
        logger.detachAppender(lignes);
    }

    private AuthenticationFailureBadCredentialsEvent echecDepuis(String identifiant, String adresse) {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.setRemoteAddr(adresse);
        Authentication tentative = new UsernamePasswordAuthenticationToken(identifiant, "peu importe");
        ((UsernamePasswordAuthenticationToken) tentative).setDetails(new WebAuthenticationDetails(requete));
        return new AuthenticationFailureBadCredentialsEvent(tentative, new BadCredentialsException("refusé"));
    }

    @Test
    void unEchecProduitUneLigneAvecLIdentifiantEtLAdresse() {
        new EcouteurEchecConnexion().consigner(echecDepuis("toto", "203.0.113.7"));

        assertThat(lignes.list).hasSize(1);
        assertThat(lignes.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(lignes.list.get(0).getFormattedMessage())
                .isEqualTo("ECHEC_CONNEXION identifiant=\"toto\" adresse=203.0.113.7");
    }

    @Test
    void uneTentativeSansAdresseNeCasseRien() {
        // Un échec peut survenir hors requête HTTP (tâche, appel interne) : la ligne doit alors
        // rester lisible plutôt que porter « null » ou faire lever une exception dans un écouteur
        // d'événement, qui remonterait dans la chaîne d'authentification.
        Authentication tentative = new UsernamePasswordAuthenticationToken("toto", "peu importe");
        new EcouteurEchecConnexion().consigner(
                new AuthenticationFailureBadCredentialsEvent(tentative, new BadCredentialsException("refusé")));

        assertThat(lignes.list).hasSize(1);
        assertThat(lignes.list.get(0).getFormattedMessage())
                .isEqualTo("ECHEC_CONNEXION identifiant=\"toto\" adresse=inconnue");
    }

    @Test
    void leMotDePasseNApparaitJamaisDansLaLigne() {
        new EcouteurEchecConnexion().consigner(echecDepuis("toto", "203.0.113.7"));

        assertThat(lignes.list.get(0).getFormattedMessage()).doesNotContain("peu importe");
    }
}
