package com.mr486.gestonote.controller;

import com.mr486.gestonote.dto.NoteDto;
import com.mr486.gestonote.service.ListeNotesService;
import com.mr486.gestonote.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur du tableau de notes : affichage, suppression, et formulaires d'édition/création.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotePageController {

    private final ListeNotesService listeNotesService;
    private final NoteService noteService;

    /**
     * Affiche le tableau des notes, éventuellement en mode édition.
     *
     * <p><b>Exemple :</b> un GET sur {@code /notes?modeEdit=true} affiche le tableau avec
     * les actions d'édition visibles.</p>
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
     * <p><b>Exemple :</b> un DELETE sur {@code /notes/delete/5} supprime la note 5.</p>
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
     * rattaché à la catégorie 2.</p>
     *
     * @param categorieId identifiant de la catégorie de rattachement
     * @param model       modèle de la vue
     * @return le nom de la vue d'édition
     */
    @GetMapping(value = "/notes/add/{categorieId}")
    public String addNote(@PathVariable Integer categorieId, Model model) {
        model.addAttribute("page_active", "edition");
        NoteDto note = new NoteDto();
        note.setCategorieId(categorieId);
        model.addAttribute("note", note);
        model.addAttribute("formAction", "/notes/add/" + categorieId);
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
    public String updateNoteForm(@PathVariable Integer id, Model model) {
        model.addAttribute("page_active", "edition");
        model.addAttribute("note", new NoteDto().fromModel(noteService.getNoteById(id)));
        model.addAttribute("formAction", "/notes/update/" + id);
        return "edition";
    }

    /**
     * Crée une note dans une catégorie puis redirige vers le tableau en mode édition.
     *
     * <p><b>Exemple :</b> un POST sur {@code /notes/add/2} avec un titre et un contenu
     * crée la note dans la catégorie 2 ; un formulaire invalide ré-affiche l'édition.</p>
     *
     * @param categorieId identifiant de la catégorie de rattachement
     * @param note        données saisies de la note
     * @param resultat    résultat de la validation
     * @param model       modèle de la vue
     * @return la redirection vers le tableau, ou la vue d'édition si la saisie est invalide
     */
    @PostMapping(value = "/notes/add/{categorieId}")
    public String addNoteSubmit(@PathVariable Integer categorieId, @Valid @ModelAttribute("note") NoteDto note,
                                BindingResult resultat, Model model) {
        if (resultat.hasErrors()) {
            model.addAttribute("formAction", "/notes/add/" + categorieId);
            return "edition";
        }
        note.setCategorieId(categorieId);
        noteService.addNote(note);
        return "redirect:/notes?modeEdit=true";
    }

    /**
     * Met à jour une note puis redirige vers le tableau en mode édition.
     *
     * <p><b>Exemple :</b> un POST sur {@code /notes/update/5} remplace le titre, la
     * couleur et le contenu de la note 5 ; un formulaire invalide ré-affiche l'édition.</p>
     *
     * @param id       identifiant de la note à modifier
     * @param note     données saisies de la note
     * @param resultat résultat de la validation
     * @param model    modèle de la vue
     * @return la redirection vers le tableau, ou la vue d'édition si la saisie est invalide
     */
    @PostMapping(value = "/notes/update/{id}")
    public String updateNoteSubmit(@PathVariable Integer id, @Valid @ModelAttribute("note") NoteDto note,
                                   BindingResult resultat, Model model) {
        if (resultat.hasErrors()) {
            model.addAttribute("formAction", "/notes/update/" + id);
            return "edition";
        }
        noteService.updateNote(id, note);
        return "redirect:/notes?modeEdit=true";
    }
}
