package com.mr486.gestonote.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "notes")
public class Note {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer categorieId=1;
    private String titre="";
    private Integer couleur=1;
    @Column(columnDefinition = "TEXT")
    private String contenu="";
}
