package com.mr486.gestonote.service;

import com.mr486.gestonote.dto.CategorieDto;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.persistance.CategorieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public List<Categorie> getAllCategoriesModifiables() {
        return categorieRepository.findAllByEstModifiableTrueOrderById();
    }

    public List<Categorie> getAllCategoriesActives() {
        return categorieRepository.findAllByEstActiveTrueOrderById();
    }

    public Categorie getCategorieById(Integer id) {
        return categorieRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Categorie not found with id: " + id));
    }

    public void updateCategorie(Integer id, CategorieDto categorieDto) {
        Categorie categorie = getCategorieById(id);
        if(!categorie.getEstModifiable()){
            throw new IllegalArgumentException("Categorie not modifiable");
        }
        categorie.setDenomination(categorieDto.getDenomination());
        categorieRepository.save(categorie);
    }

    public void categorieTrigger(Integer id) {
        Categorie categorie = getCategorieById(id);
        categorie.setEstModifiable(!categorie.getEstModifiable());
        categorieRepository.save(categorie);
    }
}
