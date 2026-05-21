package com.esmt.projet.dtos;

import com.esmt.projet.entities.DocumentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentDTO {
    private Long id;
    private String fileName;
    private DocumentType type;
    private LocalDateTime uploadDate;
}
