package com.mr486.gestonote.dto;

import com.mr486.gestonote.model.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoteDto {
    private Integer categorieId=1;
    private String titre="";
    private Integer couleur=1;
    private String contenu="";

    public NoteDto fromModel(Note note) {
        NoteDto noteDto = new NoteDto();
        noteDto.setCategorieId(note.getCategorieId());
        noteDto.setTitre(note.getTitre());
        noteDto.setCouleur(note.getCouleur());
        noteDto.setContenu(note.getContenu());
        return noteDto;
    }

    public Note toModel(Note note){
        note.setCategorieId(categorieId);
        note.setTitre(titre);
        note.setCouleur(couleur);
        note.setContenu(contenu);
        return note;
    }
}
