package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.NoteDto;
import com.mr486.gestonote.model.Note;
import com.mr486.gestonote.persistance.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    public List<Note> getAllNotesByCategorieId(Integer idCategorie){
        return noteRepository.findAllByCategorieIdOrderById(idCategorie);
    }

    public Note getNoteById(Integer id){
        return noteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + id));
    }

    public void addNote(NoteDto noteDto){
        Note note = noteDto.toModel(new Note());
        noteRepository.save(note);
    }

    public void updateNote(Integer id, NoteDto noteDto){
        Note note = noteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + id));
        note = noteDto.toModel(note);
        noteRepository.save(note);
    }

    public void deleteNote(Integer id){
        noteRepository.deleteById(id);
    }

    public @Nullable Object getNoteById(int i) {
        return noteRepository.findById(i);
    }
}
