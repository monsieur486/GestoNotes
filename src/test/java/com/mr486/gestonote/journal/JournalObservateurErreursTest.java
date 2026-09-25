package com.mr486.gestonote.journal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * L'observateur émet ce que {@link ClassementErreur} décide, et ne résout rien.
 *
 * <p>Le client est une sous-classe inerte (adresse vide : aucun fil, aucun envoi) qui
 * retient ce qu'on lui confie — ni réseau ni Mockito.</p>
 */
class JournalObservateurErreursTest {

    private final List<JournalClient.EvenementSortant> emis = new ArrayList<>();

    private final JournalClient client = new JournalClient("", "", "essai") {
        @Override
        public void emettre(JournalClient.EvenementSortant evenement) {
            emis.add(evenement);
        }
    };

    private final JournalObservateurErreurs observateur = new JournalObservateurErreurs(client);

    @AfterEach
    void fermer() {
        client.fermer();
    }

    private static MockHttpServletRequest requete(String chemin, String referer) {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", chemin);
        requete.addHeader("Host", "notes.mr486.com");
        if (referer != null) {
            requete.addHeader("Referer", referer);
        }
        return requete;
    }

    @Test
    void uneExceptionOrdinaireEmetUneErreurAvecSaTrace() {
        observateur.resolveException(requete("/reunions", null), new MockHttpServletResponse(),
                null, new IllegalStateException("boum"));

        assertThat(emis).singleElement().satisfies(evenement -> {
            assertThat(evenement.type()).isEqualTo("appli.erreur");
            assertThat(evenement.niveau()).isEqualTo("erreur");
            assertThat(evenement.details()).contains("IllegalStateException: boum");
        });
    }

    @Test
    void un404SansRefererEmetUnIntrouvableSansTrace() {
        observateur.resolveException(requete("/.env", null), new MockHttpServletResponse(), null,
                new NoResourceFoundException(HttpMethod.GET, "/.env", "/.env"));

        assertThat(emis).singleElement().satisfies(evenement -> {
            assertThat(evenement.type()).isEqualTo("appli.http.introuvable");
            assertThat(evenement.niveau()).isEqualTo("info");
            assertThat(evenement.details()).isNull();
        });
    }

    @Test
    void un404DepuisUnePageDuSiteEmetUnLienCasse() {
        observateur.resolveException(requete("/js/absent.js", "https://notes.mr486.com/reunions"),
                new MockHttpServletResponse(), null,
                new NoResourceFoundException(HttpMethod.GET, "/js/absent.js", "/js/absent.js"));

        assertThat(emis).singleElement().satisfies(evenement -> {
            assertThat(evenement.type()).isEqualTo("appli.http.lien-casse");
            assertThat(evenement.resume())
                    .isEqualTo("Lien cassé : /js/absent.js demandé depuis /reunions");
        });
    }

    @Test
    void lObservateurNeResoutRien() {
        assertThat(observateur.resolveException(requete("/", null), new MockHttpServletResponse(),
                null, new IllegalStateException())).isNull();
    }
}
