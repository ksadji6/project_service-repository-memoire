package com.esmt.projet.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.esmt.projet.dtos.DashboardStatsDTO;
import com.esmt.projet.entities.Project;
import com.esmt.projet.entities.Task;
import com.esmt.projet.entities.TaskStatus;
import com.esmt.projet.repositories.jpa.ProjectRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatistiqueService {

    @Autowired
    private ProjectRepository projectRepository;

    public DashboardStatsDTO genererDonneesDashboard() {
        List<Project> projets = projectRepository.findAll();

        // 1. Recalcul de l'avancement réel en mémoire (Sécurité Bug 0%)

        for (Project p : projets) {
            if (p.getTasks() != null && !p.getTasks().isEmpty()) {
                long terminees = p.getTasks().stream().filter(t -> t.getStatut() == TaskStatus.TERMINE).count();
                int avancementReel = (int) ((terminees * 100) / p.getTasks().size());
                p.setAvancement(avancementReel);
            }
        }

        // 2. CALCUL DES BLOCS KPI

        long totalProjets = projets.size();

        long projetsBloques = projets.stream()
                .filter(p -> p.getStatut() != null && p.getStatut().name().equals("BLOQUE"))
                .count();

        double avancementMoyenGlobal = projets.stream()
                .mapToDouble(Project::getAvancement)
                .average()
                .orElse(0.0);

        // 3. GRAPHIQUE 1 : REPARTITION PAR CATEGORIE

        Map<String, Long> repartitionParCategorie = new HashMap<>();
        repartitionParCategorie.put("SECURITE_RESEAUX", projets.stream().filter(p -> p.getCategorie() != null && p.getCategorie().name().equals("SECURITE_RESEAUX")).count());
        repartitionParCategorie.put("INFRA_SYSTEME", projets.stream().filter(p -> p.getCategorie() != null && p.getCategorie().name().equals("INFRA_SYSTEME")).count());

        // 4. GRAPHIQUE 2 : PROJETS PAR PHASE
        Map<String, Long> projetsParPhase = projets.stream()
                .filter(p -> p.getPhase() != null)
                .collect(Collectors.groupingBy(p -> p.getPhase().name(), Collectors.counting()));

        List.of("PRE_PROJET", "PROJET", "POST_PROJET").forEach(phase -> projetsParPhase.putIfAbsent(phase, 0L));

        // 5. GRAPHIQUE 3 : AVANCEMENT PAR PROJET
        Map<String, Integer> avancementParProjet = projets.stream()
                .collect(Collectors.toMap(Project::getTitre, Project::getAvancement, (v1, v2) -> v1));

        Map<String, Map<String, Long>> chargeTravailParIngenieur = new HashMap<>();

        // 6. GRAPHIQUE 4 : CHARGE DE TRAVAIL PAR INGENIEUR
        for (Project p : projets) {
            if (p.getTasks() != null) {
                for (Task t : p.getTasks()) {
                    String ingenieurKey = (t.getIngenieurId() != null) ? t.getIngenieurId().toString() : "Non assigne";
                    String statutTache = (t.getStatut() != null) ? t.getStatut().name() : "A_FAIRE";

                    chargeTravailParIngenieur.putIfAbsent(ingenieurKey, new HashMap<>());
                    Map<String, Long> statsStatuts = chargeTravailParIngenieur.get(ingenieurKey);

                    statsStatuts.put(statutTache, statsStatuts.getOrDefault(statutTache, 0L) + 1);
                }
            }
        }
        Map<String, Long> tendance = genererTendanceAvancement();

        // On retourne le DTO
        return new DashboardStatsDTO(totalProjets, projetsBloques, avancementMoyenGlobal,
                repartitionParCategorie, projetsParPhase, avancementParProjet, chargeTravailParIngenieur, tendance);
    }
    // Dans StatistiqueService.java
    public Map<String, Long> genererTendanceAvancement() {
        return projectRepository.findAll().stream()
                .flatMap(p -> p.getTasks().stream())
                .filter(t -> t.getStatut() == TaskStatus.TERMINE && t.getDateCreation() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getDateCreation().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()
                ));
    }
}