package com.mr486.gestonote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Données de saisie d'une catégorie (formulaire de renommage / activation).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategorieDto {

    /** Libellé saisi de la catégorie. */
    private String denomination;

    /** Indique si la catégorie doit être active. */
    private Boolean estActive;
}
