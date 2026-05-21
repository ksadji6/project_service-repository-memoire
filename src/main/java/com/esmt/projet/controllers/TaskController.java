package com.esmt.projet.controllers;

import com.esmt.projet.dtos.ProjectResponseDTO;
import com.esmt.projet.dtos.TaskDTO;
import com.esmt.projet.entities.TaskStatus;
import com.esmt.projet.services.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tâches Opérationnelles", description = "Gestion des jalons et tâches affectées aux ingénieurs techniques de CIS")
public class TaskController {

    private final ProjectService projectService;

    //ajouter une tache à un projet
    @PostMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    @Operation(summary = "Ajouter une tâche à un projet", description = "Crée un nouveau jalon opérationnel lié à un projet et l'affecte à un identifiant ingénieur.")
    public ResponseEntity<ProjectResponseDTO> createTask(@PathVariable Long projectId,
                                                         @RequestBody TaskDTO taskDTO)
    {
        return ResponseEntity.ok(projectService.ajouterTask(projectId,taskDTO));
    }

    //modifier le statut d'une tache et recalculer autoomatiquement l'avancement
    @PutMapping("/{taskId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    @Operation(summary = "Modifier le statut d'une tâche", description = "Bascule l'état (A_FAIRE, EN_COURS, TERMINE) d'une tâche et déclenche automatiquement le recalcul de l'avancement global du projet parent.")
    public ResponseEntity<ProjectResponseDTO> updateTaskStatus(@PathVariable Long taskId,
                                                               @RequestParam TaskStatus status)
    {
        return ResponseEntity.ok(projectService.changerStatutTask(taskId, status));
    }


}
