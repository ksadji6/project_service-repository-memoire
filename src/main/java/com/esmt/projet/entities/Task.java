package com.esmt.projet.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tasks")
public class Task {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String intitule;

    @Enumerated(EnumType.STRING)
    private TaskStatus statut;

    private Long ingenieurId; //id de l'ingenieur affecté à la tâche
    private LocalDateTime dateCreation;
    private LocalDateTime dateFin;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;


}
