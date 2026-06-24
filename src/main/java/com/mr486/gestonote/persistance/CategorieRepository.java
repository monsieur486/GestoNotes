package com.mr486.gestonote.persistance;

import com.mr486.gestonote.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Integer> {
    List<Categorie> findAllByEstActiveTrueOrderById();

    List<Categorie> findAllByEstModifiableTrueOrderById();
}
