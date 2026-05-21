package com.esmt.projet.controllers;

import com.esmt.projet.dtos.ProjectRequestDTO;
import com.esmt.projet.dtos.ProjectResponseDTO;
import com.esmt.projet.services.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projets", description = "Gestion du cycle de vie des projets (Création, Lancement, Clôture et Exports)")
public class ProjectController {
    private final ProjectService projectService;
    //creer un projet
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Créer un nouveau projet", description = "Initialise un dossier de projet au statut PRE_PROJET. Accès réservé aux Administrateurs et Chefs de Projet.")
    public ResponseEntity<ProjectResponseDTO> createProject(@RequestBody ProjectRequestDTO requestDTO) {
        ProjectResponseDTO responseDTO = projectService.creerProjet(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    //valider prerequis et laner le projet : PRE_PROJET==>PROJET
    @PutMapping("/{projectId}/lancer")
    @PreAuthorize("hasAnyRole('CHEF_PROJET','ADMIN')")
    @Operation(summary = "Valider les prérequis et lancer le projet", description = "Fait passer la phase du projet de PRE_PROJET à PROJET après vérification réglementaire.")
    public ResponseEntity<ProjectResponseDTO> lancerProject(@PathVariable Long projectId) {
        ProjectResponseDTO response=projectService.validerPrerequisEtLancer(projectId);
        return ResponseEntity.ok(response);
    }

    //clôturer définitivement le projet
    @PutMapping("/{projectId}/cloturer")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Clôturer définitivement un projet", description = "Verrouille le projet et bascule sa phase en POST_PROJET après livraison client.")
    public ResponseEntity<ProjectResponseDTO> finaliserProjet(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.cloturerProjet(projectId));
    }

    //export excel
    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET','SUPERVISEUR')")
    @Operation(summary = "Télécharger le rapport Excel global 2026", description = "Génère un fichier de suivi dynamique contenant les feuilles de calcul, les KPI et les graphiques d'analyse.")
    public ResponseEntity<byte[]> downloadExcelReport() throws Exception {

        //on récupère le tableau d'octets
        byte[] fichierExcel = projectService.exporterStatistiquesProjets2026();

        //on prépare la réponse HTTP spéciale pour un téléchargement
        return ResponseEntity.ok()
                // Dis au navigateur/Postman que c'est un fichier à télécharger nommé "Rapport_CIS.xlsx"
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Rapport_CIS.xlsx")
                // Dis que le format est du vrai Excel (MIME Type)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                // On injecte le fichier dans le corps de la réponse
                .body(fichierExcel);
    }


}
