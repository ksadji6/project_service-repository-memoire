package com.esmt.projet.controllers;

import com.esmt.projet.dtos.ProjectResponseDTO;
import com.esmt.projet.dtos.TaskDTO;
import com.esmt.projet.entities.Task;
import com.esmt.projet.entities.TaskStatus;
import com.esmt.projet.services.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/tasks")
@RequiredArgsConstructor
@Tag(name = "Tâches Opérationnelles", description = "Gestion des jalons et tâches affectées aux ingénieurs techniques de CIS")
public class TaskController {

    private final ProjectService projectService;

    //ajouter une tache à un projet
    @PostMapping("/project/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    @Operation(summary = "Ajouter une tâche à un projet", description = "Crée un nouveau jalon opérationnel lié à un projet et l'affecte à un identifiant ingénieur.")
    public ResponseEntity<?> createTask(@PathVariable Long projectId, @RequestBody TaskDTO taskDTO) {
        try {
            System.out.println("DEBUG: Appel reçu pour projet ID = " + projectId);
            projectService.ajouterTask(projectId, taskDTO);

            // Retourne un simple message JSON, sans boucle infinie
            return ResponseEntity.ok(Map.of("message", "Tâche créée avec succès"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    /*public ProjectResponseDTO createTask(@PathVariable Long projectId,
                                                         @RequestBody TaskDTO taskDTO)
    {
        System.out.println("DEBUG: Appel reçu pour projet ID = " + projectId);
        return projectService.ajouterTask(projectId,taskDTO);
    }*/

    //modifier le statut d'une tache et recalculer autoomatiquement l'avancement
    @PutMapping("/{taskId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    @Operation(summary = "Modifier le statut d'une tâche", description = "Bascule l'état (A_FAIRE, EN_COURS, TERMINE) d'une tâche et déclenche automatiquement le recalcul de l'avancement global du projet parent.")
    public ResponseEntity<ProjectResponseDTO> updateTaskStatus(@PathVariable Long taskId,
                                                               @RequestParam TaskStatus status)
    {
        return ResponseEntity.ok(projectService.changerStatutTask(taskId, status));
    }


    @GetMapping("/ingenieur/{ingenieurId}")
    public ResponseEntity<List<Task>> getTachesByIngenieur(@PathVariable Long ingenieurId) {
        return ResponseEntity.ok(projectService.findByIngenieurId(ingenieurId));
    }



}
