package com.mr486.gestonote.persistance;

import com.mr486.gestonote.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Integer> {
    List<Note> findAllByCategorieIdOrderById(Integer idCategorie);
}
