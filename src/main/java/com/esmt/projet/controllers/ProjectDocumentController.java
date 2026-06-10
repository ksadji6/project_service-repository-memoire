package com.esmt.projet.controllers;

import com.esmt.projet.dtos.DocumentDTO;
import com.esmt.projet.entities.DocumentType;
import com.esmt.projet.entities.ProjectDocument;
import com.esmt.projet.services.ProjectDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/projects/documents")
@RequiredArgsConstructor
@Tag(name = "Documents (MongoDB)", description = "Dépôt documentaire et archivage des livrables (CDC, PV, Rapport d'audit) dans MongoDB GridFS")
public class ProjectDocumentController {
        private final ProjectDocumentService documentService;

        //upload un document pour un projet specifique --> regles; seuls les presales et les ingenieurs peuvent le faire
        @PostMapping("/upload/{projectId}")
        @PreAuthorize("hasAnyRole('PRESALES', 'INGENIEUR','CHEF_PROJET')")
        @Operation(summary = "Téléverser un document de projet", description = "Permet aux ingénieurs et avant-ventes de lier des pièces jointes techniques (PDF, images) à un projet.")
        public ResponseEntity<String> uploadDocument(@PathVariable Long projectId,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam("type")DocumentType type) throws IOException

        {
            documentService.uploadDocument(projectId, file, type);
            return  ResponseEntity.ok("Document" + type + " uploadé avec succès dans MongoDB!");
        }

        @GetMapping("/download/{documentId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'PRESALES', 'CHEF_PROJET', 'INGENIEUR', 'SUPERVISEUR')")
        @Operation(summary = "Télécharger un document par son ID", description = "Récupère le flux binaire d'un document stocké pour consultation ou impression.")
        public ResponseEntity<byte[]> downloadDocument(@PathVariable String documentId) {
            ProjectDocument document = documentService.getDocument(documentId);

            // 1. Protection contre les données nulles
            if (document.getData() == null) {
                return ResponseEntity.notFound().build();
            }

            // 2. On renvoie le binaire pur
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getFileName() + "\"")
                    // APPLICATION_OCTET_STREAM est vital : il dit au navigateur "c'est du binaire, ne cherche pas à l'ouvrir"
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(document.getData().length) // On donne la taille exacte
                    .body(document.getData());
        }


        @DeleteMapping("/delete/{documentId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'PRESALES', 'INGENIEUR')")
        @Operation(summary = "Supprimer un document", description = "Seul l'auteur du document ou l'ADMIN peut effectuer cette action")
        public ResponseEntity<String> deleteDocument(@PathVariable String documentId) {
            documentService.deleteDocument(documentId);
            return ResponseEntity.ok("Le document a été supprimé avec succès !");
        }

    // Dans ProjectDocumentController.java

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRESALES', 'CHEF_PROJET', 'INGENIEUR', 'SUPERVISEUR')")
    @Operation(summary = "Lister les documents d'un projet")
    public ResponseEntity<List<DocumentDTO>> getDocumentsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(documentService.getDocumentsByProjectId(projectId));
    }


}
