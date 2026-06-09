package com.esmt.projet.controllers;

import com.esmt.projet.client.IdentityClient;
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

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projets", description = "Gestion du cycle de vie des projets (Création, Lancement, Clôture et Exports)")
public class ProjectController {
    private final ProjectService projectService;
    private final IdentityClient identityClient;
    //creer un projet
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Créer un nouveau projet", description = "Initialise un dossier de projet au statut PRE_PROJET. Accès réservé aux Administrateurs et Chefs de Projet.")
    public ResponseEntity<?> createProject(@RequestBody ProjectRequestDTO requestDTO) {
        try {
            projectService.creerProjet(requestDTO);
            return ResponseEntity.ok(Map.of("message", "Projet créé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    /*public ResponseEntity<?> createProject(@RequestBody ProjectRequestDTO requestDTO) {
        try {
            // Le service fait le travail (Insert en BD + Mail)
            projectService.creerProjet(requestDTO);

            // On retourne un statut HTTP 201 sans essayer de convertir l'objet Java en JSON
            // C'est ce qui évite la boucle infinie de sérialisation
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    public ResponseEntity<?> createProject(@RequestBody ProjectRequestDTO requestDTO) {
        try {
            // On exécute la logique
            ProjectResponseDTO savedProject = projectService.creerProjet(requestDTO);

            // On ne retourne pas l'objet complet, mais on s'assure de retourner un JSON simple
            // L'id seul suffit au front pour rediriger
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", savedProject.getId(), "message", "Projet créé"));
        } catch (Exception e) {
            // Ici tu verras le VRAI message d'erreur si ça plante
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }*/
    //rechercher un projet à travers son id
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'PRESALES', 'INGENIEUR', 'SUPERVISEUR')")
    @Operation(summary = "Récupérer un projet par son ID", description = "Retourne le ProjectResponseDTO complet avec ses tâches et ses documents")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.rechercherProjectById(id));
    }

    // recuperer tous les projets de la bdd
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'PRESALES', 'INGENIEUR', 'SUPERVISEUR')")
    @Operation(summary = "Récupérer tous les projets", description = "Retourne la liste complète de tous les projets de la base de données.")
    public ResponseEntity<java.util.List<ProjectResponseDTO>> getAllProjects() {
        return ResponseEntity.ok(projectService.rechercherTousLesProjets());
    }

    @PutMapping("/{projectId}/prerequis/{prerequisId}/toggle")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Basculer l'état d'un prérequis")
    public ResponseEntity<?> togglePrerequis(@PathVariable Long projectId, @PathVariable Long prerequisId) {
        // On délègue la logique au service
        projectService.togglePrerequisStatus(projectId, prerequisId);
        return ResponseEntity.ok(Map.of("message", "Prérequis mis à jour avec succès"));
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

    @GetMapping("/mes-projets/{idChef}")
    public ResponseEntity<List<ProjectResponseDTO>> getMesProjets(@PathVariable Long idChef) {
        // Ici, pas besoin d'appeler IdentityClient pour convertir l'email
        return ResponseEntity.ok(projectService.findByChefProjetId(idChef));
    }

    @GetMapping("/ingenieur/{ingenieurId}")
    @PreAuthorize("hasRole('INGENIEUR')")
    public ResponseEntity<List<ProjectResponseDTO>> getProjetsParIngenieur(@PathVariable Long ingenieurId) {
        // Il faudra créer cette méthode dans ProjectService
        return ResponseEntity.ok(projectService.findProjectsByIngenieurId(ingenieurId));
    }




}
