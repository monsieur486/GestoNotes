package com.mr486.gestonote.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de validation Bean de {@link NoteDto}.
 */
class NoteDtoValidationTest {

    // Indique si un champ donné est en violation de contrainte pour ce DTO.
    private boolean champEnViolation(NoteDto dto, String champ) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(dto).stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals(champ));
        }
    }

    @Test
    void titreVideEstRejete() {
        NoteDto dto = NoteDto.builder().categorieId(1).titre("  ").couleur(1).contenu("texte").build();
        assertThat(champEnViolation(dto, "titre")).isTrue();
    }

    @Test
    void contenuVideEstRejete() {
        NoteDto dto = NoteDto.builder().categorieId(1).titre("Titre").couleur(1).contenu("").build();
        assertThat(champEnViolation(dto, "contenu")).isTrue();
    }

    @Test
    void noteCompleteEstValide() {
        NoteDto dto = NoteDto.builder().categorieId(1).titre("Titre").couleur(1).contenu("texte").build();
        assertThat(champEnViolation(dto, "titre")).isFalse();
        assertThat(champEnViolation(dto, "contenu")).isFalse();
    }
}
