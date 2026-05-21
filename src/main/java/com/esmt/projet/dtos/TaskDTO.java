package com.esmt.projet.dtos;

import com.esmt.projet.entities.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskDTO {
    private Long id;
    private String intitule;
    private TaskStatus statut;
    private Long ingenieurId; //qui fait la tache
    private LocalDateTime dateCreation;
    private LocalDateTime delais;
}
