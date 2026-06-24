package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.CategorieDto;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.persistance.CategorieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service métier de gestion des catégories : lecture, renommage et bascule de l'état modifiable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategorieService {

    private final CategorieRepository categorieRepository;

    /**
     * Liste les catégories modifiables, triées par identifiant croissant.
     *
     * <p><b>Exemple :</b> {@code getAllCategoriesModifiables()} exclut les catégories
     * système (non modifiables) comme « Principale ».</p>
     *
     * @return la liste des catégories modifiables (jamais {@code null})
     */
    public List<Categorie> getAllCategoriesModifiables() {
        return categorieRepository.findAllByEstModifiableTrueOrderById();
    }

    /**
     * Liste les catégories actives, triées par identifiant croissant.
     *
     * <p><b>Exemple :</b> {@code getAllCategoriesActives()} retourne les catégories
     * affichées dans le tableau de notes, masquant les catégories désactivées.</p>
     *
     * @return la liste des catégories actives (jamais {@code null})
     */
    public List<Categorie> getAllCategoriesActives() {
        return categorieRepository.findAllByEstActiveTrueOrderById();
    }

    /**
     * Récupère une catégorie par son identifiant.
     *
     * <p><b>Exemple :</b> {@code getCategorieById(2)} retourne la catégorie d'id 2 ; un
     * identifiant inexistant lève {@link IllegalArgumentException}.</p>
     *
     * @param id identifiant de la catégorie
     * @return la catégorie correspondante
     * @throws IllegalArgumentException si aucune catégorie ne correspond à l'identifiant
     */
    public Categorie getCategorieById(Integer id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable pour l'identifiant : " + id));
    }

    /**
     * Renomme une catégorie modifiable.
     *
     * <p><b>Exemple :</b> {@code updateCategorie(2, dto)} remplace le libellé de la
     * catégorie 2 ; tenter de modifier une catégorie non modifiable lève
     * {@link IllegalArgumentException}.</p>
     *
     * @param id           identifiant de la catégorie à renommer
     * @param categorieDto nouvelles données de la catégorie
     * @throws IllegalArgumentException si la catégorie est introuvable ou non modifiable
     */
    public void updateCategorie(Integer id, CategorieDto categorieDto) {
        Categorie categorie = getCategorieById(id);
        if (!categorie.getEstModifiable()) {
            log.warn("tentative de modification de la catégorie non modifiable {}", id);
            throw new IllegalArgumentException("Catégorie non modifiable : " + id);
        }
        categorie.setDenomination(categorieDto.getDenomination());
        categorieRepository.save(categorie);
        log.info("catégorie {} renommée", id);
    }

    /**
     * Bascule l'état modifiable d'une catégorie (verrouille ou déverrouille son libellé).
     *
     * <p><b>Exemple :</b> sur une catégorie modifiable, {@code categorieTrigger(2)} la
     * rend non modifiable ; un second appel la rend de nouveau modifiable.</p>
     *
     * @param id identifiant de la catégorie
     * @throws IllegalArgumentException si aucune catégorie ne correspond à l'identifiant
     */
    public void categorieTrigger(Integer id) {
        Categorie categorie = getCategorieById(id);
        categorie.setEstModifiable(!categorie.getEstModifiable());
        categorieRepository.save(categorie);
        log.info("état modifiable de la catégorie {} basculé à {}", id, categorie.getEstModifiable());
    }
}
