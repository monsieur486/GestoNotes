package com.mr486.gestonote.persistance;

import com.mr486.gestonote.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Dépôt Spring Data des notes.
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, Integer> {

    /**
     * Liste les notes d'une catégorie, triées par identifiant croissant.
     *
     * <p><b>Exemple :</b> {@code findAllByCategorieIdOrderById(2)} retourne les notes de
     * la catégorie 2 dans l'ordre de création.</p>
     *
     * @param idCategorie identifiant de la catégorie
     * @return les notes de la catégorie, triées par identifiant
     */
    List<Note> findAllByCategorieIdOrderById(Integer idCategorie);
}
