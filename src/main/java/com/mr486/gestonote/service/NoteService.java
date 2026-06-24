package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.NoteDto;
import com.mr486.gestonote.model.Note;
import com.mr486.gestonote.persistance.CategorieRepository;
import com.mr486.gestonote.persistance.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service métier de gestion des notes : lecture, création, mise à jour et suppression.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;
    private final CategorieRepository categorieRepository;

    /**
     * Liste les notes d'une catégorie, triées par identifiant croissant.
     *
     * <p><b>Exemple :</b> {@code getAllNotesByCategorieId(2)} retourne les notes de la
     * catégorie 2 ; une catégorie sans note retourne une liste vide.</p>
     *
     * @param idCategorie identifiant de la catégorie
     * @return la liste des notes de la catégorie (jamais {@code null})
     */
    public List<Note> getAllNotesByCategorieId(Integer idCategorie) {
        return noteRepository.findAllByCategorieIdOrderById(idCategorie);
    }

    /**
     * Récupère une note par son identifiant.
     *
     * <p><b>Exemple :</b> {@code getNoteById(5)} retourne la note d'id 5 ; un identifiant
     * inexistant lève {@link IllegalArgumentException}.</p>
     *
     * @param id identifiant de la note
     * @return la note correspondante
     * @throws IllegalArgumentException si aucune note ne correspond à l'identifiant
     */
    public Note getNoteById(Integer id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note introuvable pour l'identifiant : " + id));
    }

    /**
     * Crée une nouvelle note à partir des données saisies.
     *
     * <p><b>Exemple :</b> {@code addNote(noteDto)} persiste une note dans la catégorie
     * portée par le DTO et la rend visible dans le tableau.</p>
     *
     * @param noteDto données de la note à créer
     * @throws IllegalArgumentException si la catégorie de rattachement n'existe pas
     */
    public void addNote(NoteDto noteDto) {
        verifierCategorieExiste(noteDto.getCategorieId());
        Note note = noteDto.toModel(new Note());
        noteRepository.save(note);
        log.info("note créée dans la catégorie {}", note.getCategorieId());
    }

    /**
     * Met à jour une note existante avec les données saisies.
     *
     * <p><b>Exemple :</b> {@code updateNote(5, noteDto)} remplace le titre, la couleur et
     * le contenu de la note 5 ; un identifiant inexistant lève {@link IllegalArgumentException}.</p>
     *
     * @param id      identifiant de la note à modifier
     * @param noteDto nouvelles données de la note
     * @throws IllegalArgumentException si la note ou la catégorie de rattachement n'existe pas
     */
    public void updateNote(Integer id, NoteDto noteDto) {
        verifierCategorieExiste(noteDto.getCategorieId());
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note introuvable pour l'identifiant : " + id));
        note = noteDto.toModel(note);
        noteRepository.save(note);
        log.info("note {} mise à jour", id);
    }

    /**
     * Supprime une note par son identifiant.
     *
     * <p><b>Exemple :</b> {@code deleteNote(5)} retire la note 5 ; l'appel est sans effet
     * si la note n'existe pas.</p>
     *
     * @param id identifiant de la note à supprimer
     */
    public void deleteNote(Integer id) {
        if (!noteRepository.existsById(id)) {
            log.warn("suppression ignorée : note {} introuvable", id);
            return;
        }
        noteRepository.deleteById(id);
        log.info("note {} supprimée", id);
    }

    // Vérifie l'existence de la catégorie de rattachement avant de persister une note.
    private void verifierCategorieExiste(Integer categorieId) {
        if (categorieId == null || !categorieRepository.existsById(categorieId)) {
            throw new IllegalArgumentException("Catégorie introuvable pour l'identifiant : " + categorieId);
        }
    }
}
