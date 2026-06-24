package com.mr486.gestonote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoteHtml {
    private Integer id;
    private Integer categorieId;
    private String titre;
    private String couleur;
    private String contenu;
}
