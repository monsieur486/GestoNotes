package com.mr486.gestonote.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests des entités du modèle ({@link Categorie}, {@link Note}).
 */
class EntitesTest {

    @Test
    void categorieExposeSesAccesseursEtSonBuilder() {
        Categorie categorie = Categorie.builder()
                .id(2)
                .denomination("Idées")
                .estActive(true)
                .estModifiable(true)
                .build();

        assertThat(categorie.getId()).isEqualTo(2);
        assertThat(categorie.getDenomination()).isEqualTo("Idées");
        assertThat(categorie.getEstActive()).isTrue();
        assertThat(categorie.getEstModifiable()).isTrue();

        Categorie autre = new Categorie();
        autre.setDenomination("Idées");
        assertThat(autre.getDenomination()).isEqualTo("Idées");
        assertThat(categorie.toString()).contains("Idées");
    }

    @Test
    void noteUtiliseSesValeursParDefautEtSonBuilder() {
        Note parDefaut = new Note();
        assertThat(parDefaut.getCategorieId()).isEqualTo(1);
        assertThat(parDefaut.getCouleur()).isEqualTo(1);
        assertThat(parDefaut.getTitre()).isEmpty();
        assertThat(parDefaut.getContenu()).isEmpty();

        Note note = Note.builder()
                .id(5)
                .categorieId(2)
                .titre("Courses")
                .couleur(2)
                .contenu("Pain, lait")
                .build();

        assertThat(note.getId()).isEqualTo(5);
        assertThat(note.getCategorieId()).isEqualTo(2);
        assertThat(note.getTitre()).isEqualTo("Courses");
        assertThat(note.getCouleur()).isEqualTo(2);
        assertThat(note.getContenu()).isEqualTo("Pain, lait");
        assertThat(note.toString()).contains("Courses");
    }
}
