package com.esmt.projet.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // Ajoute cette annotation Lombok
@NoArgsConstructor
public class PrerequisDTO {
    private Long id;
    private String libelle;
    private boolean estDisponible;

}
