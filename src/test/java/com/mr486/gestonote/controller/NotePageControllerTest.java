package com.mr486.gestonote.controller;

import com.mr486.gestonote.configuration.SecurityConfiguration;
import com.mr486.gestonote.dto.NoteDto;
import com.mr486.gestonote.model.Note;
import com.mr486.gestonote.service.ListeNotesService;
import com.mr486.gestonote.service.NoteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests web de {@link NotePageController}, sécurité incluse.
 */
@WebMvcTest(controllers = NotePageController.class,
        properties = {"app.auth.user01.name=test", "app.auth.user01.password=test"})
@Import(SecurityConfiguration.class)
class NotePageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListeNotesService listeNotesService;

    @MockitoBean
    private NoteService noteService;

    @Test
    void accesNonAuthentifieRedirigeVersLaConnexion() throws Exception {
        mockMvc.perform(get("/notes"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void afficheLeTableauDesNotes() throws Exception {
        com.mr486.gestonote.dto.NoteHtml note = com.mr486.gestonote.dto.NoteHtml.builder()
                .id(5).categorieId(2).titre("Courses").couleur("btn btn-success").contenu("Pain, lait").build();
        com.mr486.gestonote.dto.CategorieHtml categorie = com.mr486.gestonote.dto.CategorieHtml.builder()
                .id(2).denomination("Idées").notes(java.util.List.of(note)).build();
        when(listeNotesService.getTableau()).thenReturn(java.util.List.of(categorie));

        mockMvc.perform(get("/notes"))
                .andExpect(status().isOk())
                .andExpect(view().name("notes"))
                .andExpect(model().attribute("modeEdit", false));
    }

    @Test
    @WithMockUser
    void afficheLeTableauEnModeEdition() throws Exception {
        when(listeNotesService.getTableau()).thenReturn(List.of());

        mockMvc.perform(get("/notes").param("modeEdit", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("modeEdit", true));
    }

    @Test
    @WithMockUser
    void supprimeUneNotePuisRedirige() throws Exception {
        mockMvc.perform(delete("/notes/delete/5").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes"));

        verify(listeNotesService).deleteNote(5L);
    }

    @Test
    @WithMockUser
    void afficheLeFormulaireDAjout() throws Exception {
        mockMvc.perform(get("/notes/add/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"))
                .andExpect(model().attributeExists("note", "formAction"));
    }

    @Test
    @WithMockUser
    void afficheLeFormulaireDeModification() throws Exception {
        when(noteService.getNoteById(5))
                .thenReturn(Note.builder().id(5).categorieId(2).titre("T").couleur(2).contenu("C").build());

        mockMvc.perform(get("/notes/update/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"))
                .andExpect(model().attributeExists("note", "formAction"));
    }

    @Test
    @WithMockUser
    void creeUneNotePuisRedirige() throws Exception {
        mockMvc.perform(post("/notes/add/2").with(csrf())
                        .param("titre", "Courses").param("couleur", "2").param("contenu", "Pain"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes?modeEdit=true"));

        verify(noteService).addNote(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void refuseLaCreationSiTitreVide() throws Exception {
        mockMvc.perform(post("/notes/add/2").with(csrf())
                        .param("titre", "").param("couleur", "2").param("contenu", "Pain"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"));

        verify(noteService, org.mockito.Mockito.never()).addNote(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void metAJourUneNotePuisRedirige() throws Exception {
        mockMvc.perform(post("/notes/update/5").with(csrf())
                        .param("categorieId", "2").param("titre", "T2").param("couleur", "3").param("contenu", "C2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes?modeEdit=true"));

        ArgumentCaptor<NoteDto> captor = ArgumentCaptor.forClass(NoteDto.class);
        verify(noteService).updateNote(org.mockito.ArgumentMatchers.eq(5), captor.capture());
        NoteDto captured = captor.getValue();
        assertThat(captured.getCategorieId()).isEqualTo(2);
        assertThat(captured.getTitre()).isEqualTo("T2");
        assertThat(captured.getCouleur()).isEqualTo(3);
        assertThat(captured.getContenu()).isEqualTo("C2");
    }
}
