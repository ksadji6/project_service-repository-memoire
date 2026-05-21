package com.esmt.projet.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "project_documents") //collection mongodb
@Data
public class ProjectDocument {
    @Id
    private String id; //mongodb utilise des string pour l'id --> généré
    private String fileName;
    private DocumentType type;
    private Long projectId; //id du projet dans mysql pour faire le lien entre les deux
    private String uploadedBy; // Stockera l'email ou le username de l'auteur
    private LocalDateTime uploadDate;

    //contenu du fichier stocké en octets
    private byte[] data;



}
