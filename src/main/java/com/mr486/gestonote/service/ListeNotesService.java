package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.CategorieHtml;
import com.mr486.gestonote.dto.NoteHtml;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.model.Note;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListeNotesService {

    private final NoteService noteService;
    private final CategorieService categorieService;

    public List<CategorieHtml> getTableau() {
        List<CategorieHtml> listeCategoriesHtml = new ArrayList<>();
        List<Categorie> listeCategories = categorieService.getAllCategoriesActives();
        for(Categorie categorie : listeCategories){
            if (categorie == null) continue;
            CategorieHtml categorieHtml = new CategorieHtml();
            categorieHtml.setId(categorie.getId());
            categorieHtml.setDenomination(categorie.getDenomination());
            categorieHtml.setNotes(getNotesByCategorie(categorie.getId()));
            listeCategoriesHtml.add(categorieHtml);
        }
        return listeCategoriesHtml;
    }

    private List<NoteHtml> getNotesByCategorie(Integer idCategorie){
        List<Note> listeNotes = noteService.getAllNotesByCategorieId(idCategorie);
        if (listeNotes == null) return new ArrayList<>();
        List<NoteHtml> listeNotesHtml = new ArrayList<>();
        for(Note note : listeNotes){
            if (note == null) continue;
            NoteHtml noteHtml = getNoteByModel(note);
            listeNotesHtml.add(noteHtml);
        }
        return listeNotesHtml;
    }

    private String getColor(Integer code){
        if (code == null || code == 1) {
            return "btn btn-primary";
        }
        if (code == 2) {
            return "btn btn-success";
        }
        if (code == 3) {
            return "btn btn-warning";
        }
        if (code == 4) {
            return "btn btn-danger";
        }
        return "btn btn-primary";
    }

    public void deleteNote(Long id) {
        noteService.deleteNote(id.intValue());
    }

    public NoteHtml getNoteByModel(Note note){
        NoteHtml noteHtml = new NoteHtml();
        noteHtml.setId(note.getId());
        noteHtml.setCategorieId(note.getCategorieId());
        noteHtml.setTitre(note.getTitre());
        noteHtml.setContenu(note.getContenu());
        noteHtml.setCouleur(getColor(note.getCouleur()));
        return noteHtml;
    }
}
