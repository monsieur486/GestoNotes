package com.mr486.gestonote.controller;

import com.mr486.gestonote.dto.NoteHtml;
import com.mr486.gestonote.model.Note;
import com.mr486.gestonote.service.CategorieService;
import com.mr486.gestonote.service.ListeNotesService;
import com.mr486.gestonote.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class NotePageController {

    private final ListeNotesService listeNotesService;
    private final CategorieService categorieService;
    private final NoteService noteService;

    @GetMapping(value = "/notes")
    public String pageView(Model model, @RequestParam(required = false) Boolean modeEdit) {

        model.addAttribute("page_active", "notes");
        model.addAttribute("categories", listeNotesService.getTableau());
        model.addAttribute("modeEdit", modeEdit != null && modeEdit);

        return "notes";
    }

    @DeleteMapping(value ="/notes/delete/{id}")
    public String deleteNote(@PathVariable Long id) {
        listeNotesService.deleteNote(id);
        return "redirect:/notes";
    }

    @GetMapping(value = "/notes/add/{id}")
    public String addNote(@PathVariable Integer id, Model model) {
        model.addAttribute("page_active", "edition");
        NoteHtml note = new NoteHtml();
        note.setCategorieId(id);
        model.addAttribute("note", note);
        model.addAttribute("categories", categorieService.getAllCategoriesActives());
        return "edition";
    }

    @GetMapping(value = "/notes/update/{id}")
    public String updateNote(@PathVariable Integer id, Model model) {
        model.addAttribute("page_active", "edition");
        NoteHtml note = new NoteHtml();
        Note notedb = noteService.getNoteById(id);
        note.setCategorieId(notedb.getCategorieId());
        note.setId(notedb.getId());
        note.setTitre(notedb.getTitre());
        note.setContenu(notedb.getContenu());
        note.setCouleur(notedb.getCouleur());
        model.addAttribute("note", note);
        model.addAttribute("categories", categorieService.getAllCategoriesActives());
        return "edition";
    }
}