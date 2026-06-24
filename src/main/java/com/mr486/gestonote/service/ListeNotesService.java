package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.CategorieHtml;
import com.mr486.gestonote.dto.NoteHtml;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.model.CouleurNote;
import com.mr486.gestonote.model.Note;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service de présentation : assemble les catégories actives et leurs notes au format
 * d'affichage ({@link CategorieHtml} / {@link NoteHtml}) pour le tableau de bord.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListeNotesService {

    private final NoteService noteService;
    private final CategorieService categorieService;

    /**
     * Construit le tableau des catégories actives avec, pour chacune, ses notes prêtes
     * à l'affichage.
     *
     * <p><b>Exemple :</b> {@code getTableau()} retourne une liste de {@link CategorieHtml}
     * où chaque catégorie porte ses {@link NoteHtml} ; une catégorie sans note porte une
     * liste de notes vide.</p>
     *
     * @return les catégories actives enrichies de leurs notes (jamais {@code null})
     */
    public List<CategorieHtml> getTableau() {
        List<CategorieHtml> listeCategoriesHtml = new ArrayList<>();
        List<Categorie> listeCategories = categorieService.getAllCategoriesActives();
        for (Categorie categorie : listeCategories) {
            if (categorie == null) {
                continue;
            }
            CategorieHtml categorieHtml = new CategorieHtml();
            categorieHtml.setId(categorie.getId());
            categorieHtml.setDenomination(categorie.getDenomination());
            categorieHtml.setNotes(getNotesByCategorie(categorie.getId()));
            listeCategoriesHtml.add(categorieHtml);
        }
        log.info("tableau construit avec {} catégorie(s) active(s)", listeCategoriesHtml.size());
        return listeCategoriesHtml;
    }

    // Récupère les notes d'une catégorie et les convertit au format d'affichage.
    private List<NoteHtml> getNotesByCategorie(Integer idCategorie) {
        log.debug("récupération des notes de la catégorie {}", idCategorie);
        List<Note> listeNotes = noteService.getAllNotesByCategorieId(idCategorie);
        if (listeNotes == null) {
            return new ArrayList<>();
        }
        List<NoteHtml> listeNotesHtml = new ArrayList<>();
        for (Note note : listeNotes) {
            if (note == null) {
                continue;
            }
            listeNotesHtml.add(getNoteByModel(note));
        }
        return listeNotesHtml;
    }

    /**
     * Supprime une note à partir de son identifiant (au format {@code Long} du formulaire).
     *
     * <p><b>Exemple :</b> {@code deleteNote(5L)} supprime la note 5 ; l'appel est sans
     * effet si la note n'existe pas.</p>
     *
     * @param id identifiant de la note à supprimer
     */
    public void deleteNote(Long id) {
        noteService.deleteNote(id.intValue());
    }

    /**
     * Convertit une note du modèle de persistance vers son format d'affichage.
     *
     * <p><b>Exemple :</b> une note de couleur {@code 2} produit un {@link NoteHtml} dont
     * la couleur vaut {@code "btn btn-success"}.</p>
     *
     * @param note note du modèle à convertir
     * @return la représentation d'affichage de la note
     */
    public NoteHtml getNoteByModel(Note note) {
        NoteHtml noteHtml = new NoteHtml();
        noteHtml.setId(note.getId());
        noteHtml.setCategorieId(note.getCategorieId());
        noteHtml.setTitre(note.getTitre());
        noteHtml.setContenu(note.getContenu());
        noteHtml.setCouleur(CouleurNote.classeCss(note.getCouleur()));
        return noteHtml;
    }
}
