package com.esmt.projet.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titre;
    private String description;
    @Enumerated(EnumType.STRING)
    private ProjectCategory categorie;

    @Enumerated(EnumType.STRING)
    private ProjectPhase phase;

    @Enumerated(EnumType.STRING)
    private ProjectStatus statut;

    //ID des acteurs -> identity-service
    private Long presalesId;
    private Long chefProjetId;
    private Long superviseurId; //read-only

    private Double budget; //
    private Integer avancement; //pourcentage d'avancement
    private String commentaireBloquant; // commentaires si statut= BLOQUE

    //DELAIS
    private LocalDateTime dateDebut;
    private LocalDateTime dateFinEstimee;

    //RELATIONS

    //Liste des tâches --> phase projet
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<Task> tasks;

    //checklist des prérequis pour passer de Préprojet à Projet --> validation du chef_projet
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<Prerequis> prerequis;


}
