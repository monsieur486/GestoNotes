package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.CategorieDto;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.persistance.CategorieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service métier de gestion des catégories : lecture, renommage et bascule de l'état actif.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategorieService {

    /** Longueur maximale autorisée pour le libellé d'une catégorie. */
    private static final int MAX_DENOMINATION = 100;

    private final CategorieRepository categorieRepository;

    /**
     * Liste toutes les catégories (actives et inactives), triées par identifiant.
     *
     * <p><b>Exemple :</b> {@code getAllCategories()} alimente la page de gestion en
     * affichant aussi bien les catégories actives que désactivées.</p>
     *
     * @return la liste de toutes les catégories, triées par identifiant
     */
    public List<Categorie> getAllCategories() {
        return categorieRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
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
        String denomination = categorieDto.getDenomination() == null ? "" : categorieDto.getDenomination().trim();
        if (denomination.isEmpty() || denomination.length() > MAX_DENOMINATION) {
            log.warn("renommage refusé pour la catégorie {} : libellé invalide", id);
            throw new IllegalArgumentException("Libellé de catégorie invalide : " + id);
        }
        categorie.setDenomination(denomination);
        categorieRepository.save(categorie);
        log.info("catégorie {} renommée", id);
    }

    /**
     * Bascule l'état actif d'une catégorie (visible ou masquée dans le tableau de notes).
     *
     * <p><b>Exemple :</b> sur une catégorie active et modifiable, {@code toggleActive(2)} la
     * désactive ; un second appel la réactive. Une catégorie non modifiable et actuellement
     * active ne peut pas être désactivée.</p>
     *
     * @param id identifiant de la catégorie
     * @throws IllegalArgumentException si aucune catégorie ne correspond à l'identifiant,
     *                                  ou si la catégorie est non modifiable et active
     */
    public void toggleActive(Integer id) {
        Categorie categorie = getCategorieById(id);
        if (Boolean.FALSE.equals(categorie.getEstModifiable()) && Boolean.TRUE.equals(categorie.getEstActive())) {
            log.warn("tentative de désactivation de la catégorie non modifiable active {}", id);
            throw new IllegalArgumentException("Catégorie non modifiable active : " + id);
        }
        categorie.setEstActive(!Boolean.TRUE.equals(categorie.getEstActive()));
        categorieRepository.save(categorie);
        log.info("état actif de la catégorie {} basculé à {}", id, categorie.getEstActive());
    }
}
