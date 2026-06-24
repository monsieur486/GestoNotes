package com.mr486.gestonote.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Gestionnaire global des exceptions des contrôleurs : journalise l'erreur et redirige
 * vers le tableau des notes au lieu d'exposer la page d'erreur par défaut (et son message).
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Traite les ressources introuvables ou les opérations refusées sans divulguer de
     * détail technique à l'utilisateur.
     *
     * <p><b>Exemple :</b> ouvrir {@code /notes/update/999} pour une note inexistante
     * journalise un avertissement et redirige vers {@code /notes} en mode édition.</p>
     *
     * @param ex exception levée par un contrôleur ou un service
     * @return la redirection vers le tableau des notes
     */
    @ExceptionHandler({IllegalArgumentException.class, EmptyResultDataAccessException.class})
    public String handleRessourceIntrouvable(Exception ex) {
        log.warn("requête sur une ressource introuvable ou invalide : {}", ex.getMessage());
        return "redirect:/notes?modeEdit=true";
    }
}
