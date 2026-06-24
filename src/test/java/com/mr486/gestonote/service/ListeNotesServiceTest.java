package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.CategorieHtml;
import com.mr486.gestonote.dto.NoteHtml;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.model.Note;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link ListeNotesService}.
 */
@ExtendWith(MockitoExtension.class)
class ListeNotesServiceTest {

    @Mock
    private NoteService noteService;

    @Mock
    private CategorieService categorieService;

    @InjectMocks
    private ListeNotesService listeNotesService;

    @Test
    void getTableauAssembleLesCategoriesActivesEtLeursNotes() {
        Categorie categorie = Categorie.builder().id(2).denomination("Idées").build();
        // La liste contient un élément null pour vérifier qu'il est ignoré.
        when(categorieService.getAllCategoriesActives()).thenReturn(Arrays.asList(categorie, null));
        Note note = Note.builder().id(5).categorieId(2).titre("T").couleur(2).contenu("C").build();
        when(noteService.getAllNotesByCategorieId(2)).thenReturn(Arrays.asList(note, null));

        List<CategorieHtml> tableau = listeNotesService.getTableau();

        assertThat(tableau).hasSize(1);
        CategorieHtml categorieHtml = tableau.get(0);
        assertThat(categorieHtml.getDenomination()).isEqualTo("Idées");
        assertThat(categorieHtml.getNotes()).hasSize(1);
        assertThat(categorieHtml.getNotes().get(0).getCouleur()).isEqualTo("btn btn-success");
    }

    @Test
    void getTableauTolereUneCategorieSansNote() {
        Categorie categorie = Categorie.builder().id(2).denomination("Vide").build();
        when(categorieService.getAllCategoriesActives()).thenReturn(List.of(categorie));
        // Le service renvoie null : la catégorie doit porter une liste de notes vide.
        when(noteService.getAllNotesByCategorieId(2)).thenReturn(null);

        List<CategorieHtml> tableau = listeNotesService.getTableau();

        assertThat(tableau).hasSize(1);
        assertThat(tableau.get(0).getNotes()).isEmpty();
    }

    @Test
    void getNoteByModelResoutLaCouleurEnClasseCss() {
        Note note = Note.builder().id(5).categorieId(2).titre("T").couleur(4).contenu("C").build();

        NoteHtml html = listeNotesService.getNoteByModel(note);

        assertThat(html.getId()).isEqualTo(5);
        assertThat(html.getCouleur()).isEqualTo("btn btn-danger");
        assertThat(html.getTitre()).isEqualTo("T");
    }

    @Test
    void deleteNoteDelegueAuServiceDeNote() {
        listeNotesService.deleteNote(5L);

        verify(noteService).deleteNote(5);
    }
}
