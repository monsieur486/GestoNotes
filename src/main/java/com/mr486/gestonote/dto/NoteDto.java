package com.mr486.gestonote.dto;

import com.mr486.gestonote.model.Note;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Données de saisie d'une note (formulaire d'édition), avec conversion depuis et vers
 * le modèle de persistance {@link Note}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoteDto {

    /** Identifiant de la catégorie de rattachement (catégorie 1 par défaut). */
    @Default
    @NotNull(message = "La catégorie est obligatoire")
    private Integer categorieId = 1;

    /** Titre saisi de la note. */
    @Default
    @NotBlank(message = "Le titre est obligatoire")
    private String titre = "";

    /** Code couleur saisi de la note (entre 1 et 8, 1 par défaut). */
    @Default
    @NotNull(message = "La couleur est obligatoire")
    @Min(value = 1, message = "Couleur invalide")
    @Max(value = 8, message = "Couleur invalide")
    private Integer couleur = 1;

    /** Contenu saisi de la note. */
    @Default
    @NotBlank(message = "Le contenu est obligatoire")
    private String contenu = "";

    /**
     * Construit un DTO à partir d'une note du modèle.
     *
     * <p><b>Exemple :</b> {@code new NoteDto().fromModel(note)} recopie le titre, la
     * couleur, le contenu et la catégorie de {@code note} dans un nouveau DTO.</p>
     *
     * @param note note source du modèle
     * @return un DTO portant les valeurs de la note
     */
    public NoteDto fromModel(Note note) {
        NoteDto noteDto = new NoteDto();
        noteDto.setCategorieId(note.getCategorieId());
        noteDto.setTitre(note.getTitre());
        noteDto.setCouleur(note.getCouleur());
        noteDto.setContenu(note.getContenu());
        return noteDto;
    }

    /**
     * Reporte les valeurs du DTO dans une note du modèle.
     *
     * <p><b>Exemple :</b> {@code dto.toModel(note)} écrase le titre, la couleur, le
     * contenu et la catégorie de {@code note} avec ceux du DTO, puis retourne la note.</p>
     *
     * @param note note du modèle à alimenter
     * @return la note mise à jour
     */
    public Note toModel(Note note) {
        note.setCategorieId(categorieId);
        note.setTitre(titre);
        note.setCouleur(couleur);
        note.setContenu(contenu);
        return note;
    }
}
