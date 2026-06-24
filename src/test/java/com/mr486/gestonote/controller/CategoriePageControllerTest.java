package com.mr486.gestonote.controller;

import com.mr486.gestonote.configuration.SecurityConfiguration;
import com.mr486.gestonote.dto.CategorieDto;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.service.CategorieService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests web de {@link CategoriePageController}, sécurité incluse.
 */
@WebMvcTest(controllers = CategoriePageController.class,
        properties = {"app.auth.user01.name=test", "app.auth.user01.password=test"})
@Import(SecurityConfiguration.class)
class CategoriePageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategorieService categorieService;

    @Test
    @WithMockUser
    void afficheToutesLesCategories() throws Exception {
        when(categorieService.getAllCategories())
                .thenReturn(List.of(Categorie.builder().id(1).denomination("Principale")
                        .estActive(true).estModifiable(false).build()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Désactiver"))));
    }

    @Test
    @WithMockUser
    void basculeLEtatActif() throws Exception {
        mockMvc.perform(post("/categories/2/toggle-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categorieService).toggleActive(2);
    }

    @Test
    @WithMockUser
    void renommeUneCategorie() throws Exception {
        mockMvc.perform(post("/categories/2/rename").with(csrf()).param("denomination", "Idées"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        ArgumentCaptor<CategorieDto> captor = ArgumentCaptor.forClass(CategorieDto.class);
        verify(categorieService).updateCategorie(eq(2), captor.capture());
        assertThat(captor.getValue().getDenomination()).isEqualTo("Idées");
    }

    @Test
    @WithMockUser
    void renommeUneCategorieNonModifiableSansPlanter() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Catégorie non modifiable : 1"))
                .when(categorieService).updateCategorie(eq(1), any());

        mockMvc.perform(post("/categories/1/rename").with(csrf()).param("denomination", "X"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));
    }

    @Test
    @WithMockUser
    void basculeLEtatActifNonModifiableActiveRedirigeSansPlanter() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Catégorie non modifiable active : 1"))
                .when(categorieService).toggleActive(1);

        mockMvc.perform(post("/categories/1/toggle-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));
    }
}
