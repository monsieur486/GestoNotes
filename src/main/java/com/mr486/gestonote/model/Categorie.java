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
@Table(name = "categories")
public class Categorie {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String denomination;
    private Boolean estActive;
    private Boolean estModifiable;
}
