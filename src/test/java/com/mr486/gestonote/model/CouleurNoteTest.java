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
        assertThat(CouleurNote.classeCss(5)).isEqualTo("btn btn-secondary");
        assertThat(CouleurNote.classeCss(6)).isEqualTo("btn btn-info");
        assertThat(CouleurNote.classeCss(7)).isEqualTo("btn btn-light");
        assertThat(CouleurNote.classeCss(8)).isEqualTo("btn btn-dark");
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
        assertThat(CouleurNote.values()).hasSize(8);
        assertThat(CouleurNote.valueOf("FONCE")).isEqualTo(CouleurNote.FONCE);
    }
}
