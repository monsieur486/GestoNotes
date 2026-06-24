package com.mr486.gestonote.model;

import jakarta.persistence.Column;
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
 * Note rattachée à une catégorie (table {@code notes}).
 *
 * <p><b>Exemple :</b> une note de titre « Courses », de couleur {@code 2} (vert) et
 * rattachée à la catégorie {@code 1} ; les valeurs par défaut placent toute nouvelle
 * note dans la catégorie {@code 1} avec la couleur {@code 1}.</p>
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "notes")
public class Note {

    /** Identifiant technique, généré par la base. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Identifiant de la catégorie de rattachement (catégorie 1 par défaut). */
    private Integer categorieId = 1;

    /** Titre de la note. */
    private String titre = "";

    /** Code couleur d'affichage de la note (1 par défaut). */
    private Integer couleur = 1;

    /** Contenu libre de la note. */
    @Column(columnDefinition = "TEXT")
    private String contenu = "";
}
