package com.esmt.projet.dtos;

import com.esmt.projet.entities.ProjectCategory;
import lombok.Data;
import org.springframework.boot.convert.DataSizeUnit;

import java.time.LocalDateTime;

@Data
public class ProjectRequestDTO {
    private String titre;
    private String description;
    private ProjectCategory categorie;
    private Long presalesId;
    private Long chefProjetId;
    private Long superviseurId;
    private Double budget;
    private LocalDateTime dateFinEstimee;
}
