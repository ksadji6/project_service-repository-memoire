package com.esmt.projet.repositories;

import com.esmt.projet.entities.ProjectDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends MongoRepository<ProjectDocument, String> {
    //recuperer tous les docd'un projet mysql
    List<ProjectDocument> findByProjectId(Long projectId);
}
