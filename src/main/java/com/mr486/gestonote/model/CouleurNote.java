package com.mr486.gestonote.model;

import lombok.Getter;

/**
 * Couleur d'affichage d'une note, associant un code numérique à une classe CSS Bootstrap.
 *
 * <p><b>Exemple :</b> {@code CouleurNote.classeCss(2)} retourne {@code "btn btn-success"} ;
 * un code inconnu ou nul retombe sur la couleur par défaut {@code "btn btn-primary"}.</p>
 */
@Getter
public enum CouleurNote {

    /** Couleur par défaut (code 1). */
    PRIMAIRE(1, "btn btn-primary"),
    /** Couleur de succès (code 2). */
    SUCCES(2, "btn btn-success"),
    /** Couleur d'avertissement (code 3). */
    AVERTISSEMENT(3, "btn btn-warning"),
    /** Couleur de danger (code 4). */
    DANGER(4, "btn btn-danger"),
    /** Couleur secondaire / grise (code 5). */
    SECONDAIRE(5, "btn btn-secondary"),
    /** Couleur d'information / cyan (code 6). */
    INFO(6, "btn btn-info"),
    /** Couleur claire (code 7). */
    CLAIR(7, "btn btn-light"),
    /** Couleur foncée (code 8). */
    FONCE(8, "btn btn-dark");

    /** Code numérique stocké en base pour cette couleur. */
    private final int code;

    /** Classe CSS Bootstrap appliquée au bouton de la note. */
    private final String classeCss;

    CouleurNote(int code, String classeCss) {
        this.code = code;
        this.classeCss = classeCss;
    }

    /**
     * Retourne la classe CSS correspondant à un code couleur.
     *
     * <p><b>Exemple :</b> {@code classeCss(3)} retourne {@code "btn btn-warning"} ;
     * {@code classeCss(null)} ou un code non répertorié retourne {@code "btn btn-primary"}.</p>
     *
     * @param code code couleur de la note (peut être {@code null})
     * @return la classe CSS Bootstrap associée, ou celle de la couleur par défaut
     */
    public static String classeCss(Integer code) {
        if (code == null) {
            return PRIMAIRE.classeCss;
        }
        for (CouleurNote couleur : values()) {
            if (couleur.code == code) {
                return couleur.classeCss;
            }
        }
        return PRIMAIRE.classeCss;
    }
}
