package com.mr486.gestonote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Représentation d'affichage d'une catégorie et de ses notes pour le tableau de bord.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategorieHtml {

    /** Identifiant de la catégorie. */
    private Integer id;

    /** Libellé affiché de la catégorie. */
    private String denomination;

    /** Notes rattachées à la catégorie, au format d'affichage. */
    private List<NoteHtml> notes;
}
