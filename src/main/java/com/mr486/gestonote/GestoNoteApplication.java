package com.mr486.gestonote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application GestoNote (gestion de notes par catégories).
 */
@SpringBootApplication
public class GestoNoteApplication {

    /**
     * Démarre l'application Spring Boot.
     *
     * <p><b>Exemple :</b> {@code java -jar gestonote.jar} lance le serveur web et expose
     * l'interface sur le port configuré.</p>
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(GestoNoteApplication.class, args);
    }

}
