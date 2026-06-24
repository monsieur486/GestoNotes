package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.NoteDto;
import com.mr486.gestonote.model.Note;
import com.mr486.gestonote.persistance.CategorieRepository;
import com.mr486.gestonote.persistance.NoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link NoteService}.
 */
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private CategorieRepository categorieRepository;

    @InjectMocks
    private NoteService noteService;

    @Test
    void getAllNotesByCategorieIdDelegueAuDepot() {
        List<Note> notes = List.of(new Note());
        when(noteRepository.findAllByCategorieIdOrderById(2)).thenReturn(notes);

        assertThat(noteService.getAllNotesByCategorieId(2)).isSameAs(notes);
    }

    @Test
    void getNoteByIdRetourneLaNoteTrouvee() {
        Note note = Note.builder().id(5).build();
        when(noteRepository.findById(5)).thenReturn(Optional.of(note));

        assertThat(noteService.getNoteById(5)).isSameAs(note);
    }

    @Test
    void getNoteByIdLeveUneExceptionSiIntrouvable() {
        when(noteRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteById(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5");
    }

    @Test
    void addNotePersisteLaNoteIssueDuDto() {
        when(categorieRepository.existsById(2)).thenReturn(true);
        NoteDto dto = NoteDto.builder().categorieId(2).titre("T").couleur(2).contenu("C").build();

        noteService.addNote(dto);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertThat(captor.getValue().getTitre()).isEqualTo("T");
        assertThat(captor.getValue().getCategorieId()).isEqualTo(2);
    }

    @Test
    void addNoteLeveUneExceptionSiCategorieIntrouvable() {
        when(categorieRepository.existsById(99)).thenReturn(false);
        NoteDto dto = NoteDto.builder().categorieId(99).titre("T").couleur(2).contenu("C").build();

        assertThatThrownBy(() -> noteService.addNote(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
        verify(noteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateNoteMetAJourLaNoteExistante() {
        when(categorieRepository.existsById(3)).thenReturn(true);
        Note existante = Note.builder().id(5).titre("Ancien").build();
        when(noteRepository.findById(5)).thenReturn(Optional.of(existante));
        NoteDto dto = NoteDto.builder().categorieId(3).titre("Nouveau").couleur(1).contenu("C").build();

        noteService.updateNote(5, dto);

        verify(noteRepository).save(existante);
        assertThat(existante.getTitre()).isEqualTo("Nouveau");
        assertThat(existante.getCategorieId()).isEqualTo(3);
    }

    @Test
    void updateNoteLeveUneExceptionSiIntrouvable() {
        when(categorieRepository.existsById(1)).thenReturn(true);
        when(noteRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.updateNote(5, new NoteDto()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteNoteSupprimeLaNoteExistante() {
        when(noteRepository.existsById(5)).thenReturn(true);

        noteService.deleteNote(5);

        verify(noteRepository).deleteById(5);
    }

    @Test
    void deleteNoteIgnoreUneNoteIntrouvable() {
        when(noteRepository.existsById(5)).thenReturn(false);

        noteService.deleteNote(5);

        verify(noteRepository, never()).deleteById(5);
    }
}
