package com.mr486.gestonote.controller;

import com.mr486.gestonote.configuration.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests web des pages simples (accueil, catégories, connexion), sécurité incluse.
 */
@WebMvcTest(controllers = {HomePageController.class, CategoriePageController.class, LoginPageController.class},
        properties = {"app.auth.user01.name=test", "app.auth.user01.password=test"})
@Import(SecurityConfiguration.class)
class PagesSimplesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void afficheLAccueilSansAuthentification() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("page_active", "home"));
    }

    @Test
    void afficheLaPageDeConnexion() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("page_active", "login"));
    }

    @Test
    @WithMockUser
    void afficheLaPageDesCategoriesPourUnUtilisateurAuthentifie() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories"))
                .andExpect(model().attribute("page_active", "categories"));
    }
}
