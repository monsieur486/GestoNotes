package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.CategorieDto;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.persistance.CategorieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link CategorieService}.
 */
@ExtendWith(MockitoExtension.class)
class CategorieServiceTest {

    @Mock
    private CategorieRepository categorieRepository;

    @InjectMocks
    private CategorieService categorieService;

    @Test
    void getAllCategoriesModifiablesDelegueAuDepot() {
        List<Categorie> liste = List.of(new Categorie());
        when(categorieRepository.findAllByEstModifiableTrueOrderById()).thenReturn(liste);

        assertThat(categorieService.getAllCategoriesModifiables()).isSameAs(liste);
    }

    @Test
    void getAllCategoriesActivesDelegueAuDepot() {
        List<Categorie> liste = List.of(new Categorie());
        when(categorieRepository.findAllByEstActiveTrueOrderById()).thenReturn(liste);

        assertThat(categorieService.getAllCategoriesActives()).isSameAs(liste);
    }

    @Test
    void getCategorieByIdRetourneLaCategorieTrouvee() {
        Categorie categorie = Categorie.builder().id(2).build();
        when(categorieRepository.findById(2)).thenReturn(Optional.of(categorie));

        assertThat(categorieService.getCategorieById(2)).isSameAs(categorie);
    }

    @Test
    void getCategorieByIdLeveUneExceptionSiIntrouvable() {
        when(categorieRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categorieService.getCategorieById(2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2");
    }

    @Test
    void updateCategorieRenommeUneCategorieModifiable() {
        Categorie categorie = Categorie.builder().id(2).denomination("Ancien").estModifiable(true).build();
        when(categorieRepository.findById(2)).thenReturn(Optional.of(categorie));

        categorieService.updateCategorie(2, CategorieDto.builder().denomination("Nouveau").build());

        verify(categorieRepository).save(categorie);
        assertThat(categorie.getDenomination()).isEqualTo("Nouveau");
    }

    @Test
    void updateCategorieRefuseUneCategorieNonModifiable() {
        Categorie categorie = Categorie.builder().id(2).estModifiable(false).build();
        when(categorieRepository.findById(2)).thenReturn(Optional.of(categorie));

        assertThatThrownBy(() ->
                categorieService.updateCategorie(2, CategorieDto.builder().denomination("X").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non modifiable");
        verify(categorieRepository, never()).save(categorie);
    }

    @Test
    void categorieTriggerBasculeLEtatModifiable() {
        Categorie categorie = Categorie.builder().id(2).estModifiable(true).build();
        when(categorieRepository.findById(2)).thenReturn(Optional.of(categorie));

        categorieService.categorieTrigger(2);

        verify(categorieRepository).save(categorie);
        assertThat(categorie.getEstModifiable()).isFalse();
    }
}
