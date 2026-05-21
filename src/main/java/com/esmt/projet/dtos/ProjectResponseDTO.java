package com.esmt.projet.dtos;

import com.esmt.projet.entities.ProjectCategory;
import com.esmt.projet.entities.ProjectPhase;
import com.esmt.projet.entities.ProjectStatus;
import com.esmt.projet.entities.Task;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Data
public class ProjectResponseDTO {
    private Long id;
    private String titre;
    private String description;
    private ProjectCategory categorie;
    private ProjectPhase  phase;
    private ProjectStatus statut;
    private Integer avancement;
    private String commentaireBloquant;

    private Double budget;

    private Long presalesId;
    private Long chefProjetId;
    private Long SuperviseurId;

    private List<TaskDTO> tasks;
    private List<DocumentDTO> documents;
    private List<PrerequisDTO>  prerequis;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFinEstimee;


}
