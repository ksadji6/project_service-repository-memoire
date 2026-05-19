package com.esmt.projet.repositories;

import com.esmt.projet.entities.Prerequis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrerequisRepository extends JpaRepository<Prerequis, Long> {

}
