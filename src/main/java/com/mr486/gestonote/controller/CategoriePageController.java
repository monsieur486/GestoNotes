package com.mr486.gestonote.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur de la page de gestion des catégories.
 */
@Controller
@RequiredArgsConstructor
public class CategoriePageController {

    /**
     * Affiche la page des catégories.
     *
     * <p><b>Exemple :</b> un GET sur {@code /categories} marque l'onglet « categories »
     * actif et rend la vue {@code categories}.</p>
     *
     * @param model modèle de la vue
     * @return le nom de la vue à afficher
     */
    @GetMapping("/categories")
    public String pageView(Model model) {
        model.addAttribute("page_active", "categories");
        return "categories";
    }
}
