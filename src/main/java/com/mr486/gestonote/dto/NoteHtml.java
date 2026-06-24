package com.mr486.gestonote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représentation d'affichage d'une note destinée aux vues Thymeleaf : la couleur y est
 * déjà résolue en classe CSS Bootstrap.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoteHtml {

    /** Identifiant de la note. */
    private Integer id;

    /** Identifiant de la catégorie de rattachement. */
    private Integer categorieId;

    /** Titre de la note. */
    private String titre;

    /** Classe CSS Bootstrap d'affichage de la note (ex. {@code "btn btn-success"}). */
    private String couleur;

    /** Contenu de la note. */
    private String contenu;
}
