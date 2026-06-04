package com.esmt.projet.repositories.jpa;

import com.esmt.projet.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.project WHERE t.ingenieurId = :ingenieurId")
    List<Task> findByIngenieurId(Long ingenieurId);
}
