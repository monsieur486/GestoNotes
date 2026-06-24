package com.mr486.gestonote.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur de la page de connexion.
 */
@Controller
public class LoginPageController {

    /**
     * Affiche le formulaire de connexion.
     *
     * <p><b>Exemple :</b> un GET sur {@code /login} marque l'onglet « login » actif et
     * rend la vue {@code login}.</p>
     *
     * @param model modèle de la vue
     * @return le nom de la vue à afficher
     */
    @GetMapping(value = "/login")
    public String pageView(Model model) {
        model.addAttribute("page_active", "login");
        return "login";
    }
}
