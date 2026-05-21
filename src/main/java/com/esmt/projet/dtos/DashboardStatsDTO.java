package com.esmt.projet.dtos;

import java.util.Map;

public class DashboardStatsDTO {
    // 1. Les Blocs KPI
    private long totalProjets;
    private long projetsBloques;
    private double avancementMoyenGlobal;

    // 2. Les Graphiques Décisionnels
    private Map<String, Long> repartitionParCategorie;
    private Map<String, Long> projetsParPhase;

    // 3. Les Graphiques Techniques
    private Map<String, Integer> avancementParProjet;
    private Map<String, Map<String, Long>> chargeTravailParIngenieur;

    // Constructeurt
    public DashboardStatsDTO(long totalProjets, long projetsBloques, double avancementMoyenGlobal,
                             Map<String, Long> repartitionParCategorie, Map<String, Long> projetsParPhase,
                             Map<String, Integer> avancementParProjet, Map<String, Map<String, Long>> chargeTravailParIngenieur) {
        this.totalProjets = totalProjets;
        this.projetsBloques = projetsBloques;
        this.avancementMoyenGlobal = avancementMoyenGlobal;
        this.repartitionParCategorie = repartitionParCategorie;
        this.projetsParPhase = projetsParPhase;
        this.avancementParProjet = avancementParProjet;
        this.chargeTravailParIngenieur = chargeTravailParIngenieur;
    }

    // Getters et Setters
    public long getTotalProjets() { return totalProjets; }
    public void setTotalProjets(long totalProjets) { this.totalProjets = totalProjets; }

    public long getProjetsBloques() { return projetsBloques; }
    public void setProjetsBloques(long projetsBloques) { this.projetsBloques = projetsBloques; }

    public double getAvancementMoyenGlobal() { return avancementMoyenGlobal; }
    public void setAvancementMoyenGlobal(double avancementMoyenGlobal) { this.avancementMoyenGlobal = avancementMoyenGlobal; }

    public Map<String, Long> getRepartitionParCategorie() { return repartitionParCategorie; }
    public void setRepartitionParCategorie(Map<String, Long> repartitionParCategorie) { this.repartitionParCategorie = repartitionParCategorie; }

    public Map<String, Long> getProjetsParPhase() { return projetsParPhase; }
    public void setProjetsParPhase(Map<String, Long> projetsParPhase) { this.projetsParPhase = projetsParPhase; }

    public Map<String, Integer> getAvancementParProjet() { return avancementParProjet; }
    public void setAvancementParProjet(Map<String, Integer> avancementParProjet) { this.avancementParProjet = avancementParProjet; }

    public Map<String, Map<String, Long>> getChargeTravailParIngenieur() { return chargeTravailParIngenieur; }
    public void setChargeTravailParIngenieur(Map<String, Map<String, Long>> chargeTravailParIngenieur) { this.chargeTravailParIngenieur = chargeTravailParIngenieur; }
}