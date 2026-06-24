package com.mr486.gestonote.controller;

import com.mr486.gestonote.dto.CategorieDto;
import com.mr486.gestonote.service.CategorieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur de gestion des catégories : affichage, activation/désactivation et renommage.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CategoriePageController {

    private final CategorieService categorieService;

    /**
     * Affiche la page de gestion listant toutes les catégories.
     *
     * <p><b>Exemple :</b> un GET sur {@code /categories} affiche les catégories actives
     * et inactives avec leurs actions.</p>
     *
     * @param model modèle de la vue
     * @return le nom de la vue à afficher
     */
    @GetMapping("/categories")
    public String pageView(Model model) {
        model.addAttribute("page_active", "categories");
        model.addAttribute("categories", categorieService.getAllCategories());
        return "categories";
    }

    /**
     * Bascule l'état actif d'une catégorie puis redirige vers la page de gestion.
     *
     * <p><b>Exemple :</b> un POST sur {@code /categories/2/toggle-active} active ou
     * désactive la catégorie 2 et redirige vers {@code /categories}.</p>
     *
     * @param id identifiant de la catégorie
     * @return la redirection vers la page des catégories
     */
    @PostMapping("/categories/{id}/toggle-active")
    public String toggleActive(@PathVariable Integer id) {
        categorieService.toggleActive(id);
        log.info("bascule de l'état actif de la catégorie {} depuis l'interface", id);
        return "redirect:/categories";
    }

    /**
     * Renomme une catégorie modifiable puis redirige vers la page de gestion.
     *
     * <p><b>Exemple :</b> un POST sur {@code /categories/2/rename} avec
     * {@code denomination=Idées} renomme la catégorie 2 ; sur une catégorie non
     * modifiable, l'opération est ignorée sans erreur.</p>
     *
     * @param id           identifiant de la catégorie
     * @param denomination nouveau libellé
     * @return la redirection vers la page des catégories
     */
    @PostMapping("/categories/{id}/rename")
    public String rename(@PathVariable Integer id, @RequestParam String denomination) {
        try {
            categorieService.updateCategorie(id, CategorieDto.builder().denomination(denomination).build());
            log.info("renommage de la catégorie {} depuis l'interface", id);
        } catch (IllegalArgumentException ex) {
            log.warn("renommage refusé pour la catégorie {} : {}", id, ex.getMessage());
        }
        return "redirect:/categories";
    }
}
