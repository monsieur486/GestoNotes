package com.mr486.gestonote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategorieHtml {
    private Integer id;
    private String denomination;
    List<NoteHtml> notes;
}
