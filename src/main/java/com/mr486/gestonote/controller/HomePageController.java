package com.mr486.gestonote.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur de la page d'accueil.
 */
@Controller
public class HomePageController {

    /**
     * Affiche la page d'accueil.
     *
     * <p><b>Exemple :</b> un GET sur {@code /} marque l'onglet « home » actif et rend la
     * vue {@code home}.</p>
     *
     * @param model modèle de la vue
     * @return le nom de la vue à afficher
     */
    @GetMapping("/")
    public String pageView(Model model) {
        model.addAttribute("page_active", "home");
        return "home";
    }
}
