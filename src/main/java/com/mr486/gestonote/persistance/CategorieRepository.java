package com.mr486.gestonote.persistance;

import com.mr486.gestonote.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Dépôt Spring Data des catégories.
 */
@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Integer> {

    /**
     * Liste les catégories actives, triées par identifiant croissant.
     *
     * <p><b>Exemple :</b> {@code findAllByEstActiveTrueOrderById()} exclut les catégories
     * désactivées.</p>
     *
     * @return les catégories actives, triées par identifiant
     */
    List<Categorie> findAllByEstActiveTrueOrderById();
}
