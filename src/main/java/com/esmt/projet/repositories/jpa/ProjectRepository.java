package com.esmt.projet.repositories.jpa;

import com.esmt.projet.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findProjectById(Long projectId);
    List<Project> findByChefProjetId(Long chefProjetId);
    @Query("SELECT DISTINCT p FROM Project p JOIN p.tasks t WHERE t.ingenieurId = :ingenieurId")
    List<Project> findProjectsByIngenieurId(@Param("ingenieurId") Long ingenieurId);
}
