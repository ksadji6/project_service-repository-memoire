package com.esmt.projet.controllers;

import com.esmt.projet.dtos.DashboardStatsDTO;
import com.esmt.projet.services.StatistiqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/statistiques")
@RequiredArgsConstructor
@Tag(name = "Statistiques & Décisionnel", description = "Moteur d'agrégation de données pour l'alimentation du Dashboard Front-End")
public class StatistiqueController {

    private final StatistiqueService statistiqueService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'SUPERVISEUR')")
    @Operation(summary = "Données consolidées du Dashboard", description = "Calcule et retourne en un seul appel les blocs KPI, la répartition sécurité/infra, l'avancement des phases et la charge ingénieurs.")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        // Renvoie l'intégralité des indicateurs et graphiques calculés en une seule fois
        return ResponseEntity.ok(statistiqueService.genererDonneesDashboard());
    }
}