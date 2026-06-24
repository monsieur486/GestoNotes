package com.mr486.gestonote.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Catégorie de classement des notes (table {@code categories}).
 *
 * <p><b>Exemple :</b> une catégorie « Idées » active et modifiable regroupe des notes ;
 * une catégorie système comme « Principale » est active mais non modifiable
 * ({@code estModifiable = false}).</p>
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "categories")
public class Categorie {

    /** Identifiant technique, généré par la base. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Libellé affiché de la catégorie. */
    private String denomination;

    /** Indique si la catégorie est visible dans l'application. */
    private Boolean estActive;

    /** Indique si le libellé de la catégorie peut être modifié par l'utilisateur. */
    private Boolean estModifiable;
}
