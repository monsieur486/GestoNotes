package com.mr486.gestonote.controller;

import com.mr486.gestonote.dto.NoteHtml;
import com.mr486.gestonote.model.CouleurNote;
import com.mr486.gestonote.model.Note;
import com.mr486.gestonote.service.CategorieService;
import com.mr486.gestonote.service.ListeNotesService;
import com.mr486.gestonote.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur du tableau de notes : affichage, suppression et formulaires d'édition.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotePageController {

    private final ListeNotesService listeNotesService;
    private final CategorieService categorieService;
    private final NoteService noteService;

    /**
     * Affiche le tableau des notes, éventuellement en mode édition.
     *
     * <p><b>Exemple :</b> un GET sur {@code /notes?modeEdit=true} affiche le tableau avec
     * les actions d'édition visibles ; sans paramètre, le mode édition est désactivé.</p>
     *
     * @param model    modèle de la vue
     * @param modeEdit {@code true} pour activer le mode édition (optionnel)
     * @return le nom de la vue à afficher
     */
    @GetMapping(value = "/notes")
    public String pageView(Model model, @RequestParam(required = false) Boolean modeEdit) {
        model.addAttribute("page_active", "notes");
        model.addAttribute("categories", listeNotesService.getTableau());
        model.addAttribute("modeEdit", modeEdit != null && modeEdit);
        return "notes";
    }

    /**
     * Supprime une note puis redirige vers le tableau.
     *
     * <p><b>Exemple :</b> un DELETE sur {@code /notes/delete/5} supprime la note 5 et
     * redirige vers {@code /notes}.</p>
     *
     * @param id identifiant de la note à supprimer
     * @return la redirection vers le tableau des notes
     */
    @DeleteMapping(value = "/notes/delete/{id}")
    public String deleteNote(@PathVariable Long id) {
        listeNotesService.deleteNote(id);
        log.info("suppression de la note {} depuis l'interface", id);
        return "redirect:/notes";
    }

    /**
     * Affiche le formulaire de création d'une note dans une catégorie.
     *
     * <p><b>Exemple :</b> un GET sur {@code /notes/add/2} ouvre un formulaire vierge
     * pré-rattaché à la catégorie 2.</p>
     *
     * @param id    identifiant de la catégorie de rattachement
     * @param model modèle de la vue
     * @return le nom de la vue d'édition
     */
    @GetMapping(value = "/notes/add/{id}")
    public String addNote(@PathVariable Integer id, Model model) {
        model.addAttribute("page_active", "edition");
        NoteHtml note = new NoteHtml();
        note.setCategorieId(id);
        model.addAttribute("note", note);
        model.addAttribute("categories", categorieService.getAllCategoriesActives());
        return "edition";
    }

    /**
     * Affiche le formulaire de modification d'une note existante.
     *
     * <p><b>Exemple :</b> un GET sur {@code /notes/update/5} ouvre le formulaire
     * pré-rempli avec les données de la note 5.</p>
     *
     * @param id    identifiant de la note à modifier
     * @param model modèle de la vue
     * @return le nom de la vue d'édition
     */
    @GetMapping(value = "/notes/update/{id}")
    public String updateNote(@PathVariable Integer id, Model model) {
        model.addAttribute("page_active", "edition");
        NoteHtml note = new NoteHtml();
        Note noteEnBase = noteService.getNoteById(id);
        note.setCategorieId(noteEnBase.getCategorieId());
        note.setId(noteEnBase.getId());
        note.setTitre(noteEnBase.getTitre());
        note.setContenu(noteEnBase.getContenu());
        note.setCouleur(CouleurNote.classeCss(noteEnBase.getCouleur()));
        model.addAttribute("note", note);
        model.addAttribute("categories", categorieService.getAllCategoriesActives());
        return "edition";
    }
}
