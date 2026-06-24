package com.mr486.gestonote.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de l'énumération {@link CouleurNote}.
 */
class CouleurNoteTest {

    @Test
    void classeCssRetourneLaCouleurAttenduePourChaqueCode() {
        assertThat(CouleurNote.classeCss(1)).isEqualTo("btn btn-primary");
        assertThat(CouleurNote.classeCss(2)).isEqualTo("btn btn-success");
        assertThat(CouleurNote.classeCss(3)).isEqualTo("btn btn-warning");
        assertThat(CouleurNote.classeCss(4)).isEqualTo("btn btn-danger");
    }

    @Test
    void classeCssRetombeSurLaCouleurParDefautPourNullOuCodeInconnu() {
        assertThat(CouleurNote.classeCss(null)).isEqualTo("btn btn-primary");
        assertThat(CouleurNote.classeCss(99)).isEqualTo("btn btn-primary");
    }

    @Test
    void exposeLeCodeEtLaClasseCss() {
        assertThat(CouleurNote.SUCCES.getCode()).isEqualTo(2);
        assertThat(CouleurNote.SUCCES.getClasseCss()).isEqualTo("btn btn-success");
        assertThat(CouleurNote.values()).hasSize(4);
        assertThat(CouleurNote.valueOf("DANGER")).isEqualTo(CouleurNote.DANGER);
    }
}
