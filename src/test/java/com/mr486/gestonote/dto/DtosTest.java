package com.mr486.gestonote.dto;

import com.mr486.gestonote.model.Note;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests des objets de transfert et d'affichage.
 */
class DtosTest {

    @Test
    void noteDtoConvertitDepuisEtVersLeModele() {
        Note note = Note.builder()
                .id(5).categorieId(2).titre("Courses").couleur(3).contenu("Pain").build();

        NoteDto dto = new NoteDto().fromModel(note);
        assertThat(dto.getCategorieId()).isEqualTo(2);
        assertThat(dto.getTitre()).isEqualTo("Courses");
        assertThat(dto.getCouleur()).isEqualTo(3);
        assertThat(dto.getContenu()).isEqualTo("Pain");

        Note cible = dto.toModel(new Note());
        assertThat(cible.getCategorieId()).isEqualTo(2);
        assertThat(cible.getTitre()).isEqualTo("Courses");
        assertThat(cible.getCouleur()).isEqualTo(3);
        assertThat(cible.getContenu()).isEqualTo("Pain");
    }

    @Test
    void noteDtoBuilderEtValeursParDefaut() {
        NoteDto parDefaut = new NoteDto();
        assertThat(parDefaut.getCategorieId()).isEqualTo(1);
        assertThat(parDefaut.getCouleur()).isEqualTo(1);

        NoteDto dto = NoteDto.builder().categorieId(4).titre("T").couleur(2).contenu("C").build();
        assertThat(dto).hasToString(dto.toString());
        assertThat(dto.getTitre()).isEqualTo("T");
    }

    @Test
    void noteHtmlExposeSesChamps() {
        NoteHtml html = NoteHtml.builder()
                .id(1).categorieId(2).titre("T").couleur("btn btn-success").contenu("C").build();
        assertThat(html.getCouleur()).isEqualTo("btn btn-success");

        NoteHtml autre = new NoteHtml();
        autre.setTitre("T");
        autre.setId(1);
        autre.setCategorieId(2);
        autre.setContenu("C");
        autre.setCouleur("btn btn-success");
        assertThat(autre).isEqualTo(html);
        assertThat(autre.hashCode()).isEqualTo(html.hashCode());
    }

    @Test
    void categorieDtoExposeSesChamps() {
        CategorieDto dto = CategorieDto.builder().denomination("Idées").estActive(true).build();
        assertThat(dto.getDenomination()).isEqualTo("Idées");
        assertThat(dto.getEstActive()).isTrue();

        CategorieDto autre = new CategorieDto();
        autre.setDenomination("Idées");
        autre.setEstActive(true);
        assertThat(autre).isEqualTo(dto);
        assertThat(autre.toString()).contains("Idées");
    }

    @Test
    void categorieHtmlPorteSesNotes() {
        NoteHtml note = NoteHtml.builder().id(1).titre("T").build();
        CategorieHtml categorie = CategorieHtml.builder()
                .id(2).denomination("Idées").notes(List.of(note)).build();
        assertThat(categorie.getNotes()).containsExactly(note);

        CategorieHtml autre = new CategorieHtml();
        autre.setId(2);
        autre.setDenomination("Idées");
        autre.setNotes(List.of(note));
        assertThat(autre).isEqualTo(categorie);
        assertThat(autre.toString()).contains("Idées");
    }
}
