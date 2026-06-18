package com.esmt.projet.services;

import com.esmt.projet.client.IdentityClient;
import com.esmt.projet.dtos.DocumentDTO;
import com.esmt.projet.entities.DocumentType;
import com.esmt.projet.entities.Project;
import com.esmt.projet.entities.ProjectDocument;
import com.esmt.projet.entities.ProjectPhase;
import com.esmt.projet.exceptions.ProjectBusinessException;
import com.esmt.projet.repositories.mongodb.ProjectDocumentRepository;
import com.esmt.projet.repositories.jpa.ProjectRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectDocumentService {
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final HttpServletRequest request;
    private final IdentityClient  identityClient;
    private final NotificationService notificationService;
    Logger logger = Logger.getLogger(getClass().getName());

    public ProjectDocument uploadDocument(Long projectId, MultipartFile file, DocumentType documentType) throws IOException {

        // 1. Récupérer le projet
        Project projet = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Project non trouvé"));

        // 2. Contrôle d'accès par rôle et par type (NOUVEAU)
        if (request.isUserInRole("PRESALES")) {
            if (documentType != DocumentType.BOM && documentType != DocumentType.ARCHITECTURE) {
                throw new ProjectBusinessException("Action refusée : Vous ne pouvez uploader que BOM ou ARCHITECTURE.");
            }
        } else if (request.isUserInRole("INGENIEUR")) {
            if (documentType != DocumentType.PV_RECETTE && documentType != DocumentType.EXPLOITATION) {
                throw new ProjectBusinessException("Action refusée : Vous ne pouvez uploader que PV_RECETTE ou EXPLOITATION.");
            }
        }

        // 3. Phase PRE_PROJET
        if(projet.getPhase() == ProjectPhase.PRE_PROJET && !request.isUserInRole("PRESALES")) {
            throw new ProjectBusinessException("Action refusée : Seul un ingénieur Presales peut uploader en phase Pré-projet.");
        }

        // 4. Logique de clôture et basculement phase
        if (documentType == DocumentType.PV_RECETTE || documentType == DocumentType.EXPLOITATION) {
            if (projet.getAvancement() < 100) {
                throw new ProjectBusinessException("Action impossible : Vous ne pouvez pas uploader les documents de clôture tant que l'avancement est < 100%.");
            }

            if (!request.isUserInRole("INGENIEUR") && !request.isUserInRole("ADMIN")) {
                throw new ProjectBusinessException("Action refusée : Seul un ingénieur technique peut uploader les documents de clôture.");
            }

            if (projet.getPhase() == ProjectPhase.PROJET) {
                projet.setPhase(ProjectPhase.POST_PROJET);
                projectRepository.save(projet);

                // Ta logique de notification existante (inchangée)
                try {
                    java.util.Map<String, Object> cpMap = identityClient.getUserById(projet.getChefProjetId());
                    if (cpMap != null && cpMap.get("email") != null) {
                        String emailCP = cpMap.get("email").toString();
                        notificationService.notifierLivrablesEtCloture(emailCP, projet.getTitre(), "L'équipe technique");
                    }
                } catch (Exception e) {
                    logger.info("Erreur envoi notification bascule de phase : " + e.getMessage());
                }
            }
        }

        // 5. Phase POST_PROJET
        if(projet.getPhase() == ProjectPhase.POST_PROJET && !request.isUserInRole("INGENIEUR")) {
            throw new ProjectBusinessException("Action refusée : Seul un ingénieur technique peut uploader en Post-projet.");
        }

        // 6. Sauvegarde du document
        ProjectDocument document = new ProjectDocument();
        document.setFileName(file.getOriginalFilename());
        document.setType(documentType);
        document.setProjectId(projectId);
        document.setUploadDate(LocalDateTime.now());
        if (request.getUserPrincipal() != null) {
            document.setUploadedBy(request.getUserPrincipal().getName());
        }
        document.setData(file.getBytes());

        ProjectDocument savedDocument = projectDocumentRepository.save(document);

        // 7. Notifs de fin de méthode (ton code existant, inchangé)
        if (projet.getPhase() == ProjectPhase.PRE_PROJET) {
            try {
                java.util.Map<String, Object> cpMap = identityClient.getUserById(projet.getChefProjetId());
                if (cpMap != null && cpMap.get("email") != null) {
                    String emailCP = cpMap.get("email").toString();
                    String nomPresales = (request.getUserPrincipal() != null) ? request.getUserPrincipal().getName() : "L'ingénieur Presales";
                    notificationService.notifierNouveauPreProjet(emailCP, projet.getTitre(), nomPresales);
                }
            } catch (Exception e) {
                System.err.println("Erreur envoi alerte au Chef de Projet en phase Pré-projet : " + e.getMessage());
            }
        }

        if (documentType == DocumentType.PV_RECETTE || documentType == DocumentType.EXPLOITATION) {
            try {
                java.util.Map<String, Object> cpMap = identityClient.getUserById(projet.getChefProjetId());
                if (cpMap != null && cpMap.get("email") != null) {
                    String emailCP = cpMap.get("email").toString();
                    notificationService.notifierLivrablesEtCloture(emailCP, projet.getTitre(), "L'ingénieur technique");
                }
            } catch (Exception e) {
                System.err.println("Erreur envoi alerte dépôt document : " + e.getMessage());
            }
        }

        return savedDocument;
    }
    /*public ProjectDocument uploadDocument(Long projectId, MultipartFile file, DocumentType documentType) throws IOException {

        //recuperer le projet pour verifier sa phase actuelle
        Project projet = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Project non trouvé"));

        //phase=PRE_PROJET
        if(projet.getPhase()== ProjectPhase.PRE_PROJET && !request.isUserInRole("PRESALES"))
        {
            throw new ProjectBusinessException("Action refusée : Seul un ingénieur Presales peut uploader en phase Pré-projet.");
        }
        //phase Projet--> en cours de cloture --> passage vers post_projet
        if (documentType == DocumentType.PV_RECETTE || documentType == DocumentType.EXPLOITATION) {
            if (projet.getAvancement() < 100)
            {
                throw new ProjectBusinessException("Action impossible : Vous ne pouvez pas uploader les documents de clôture tant que l'avancement des tâches n'est pas à 100%.");
            }

            //seul l'ingénieur technique s'occupe de la clôture
            if (!request.isUserInRole("INGENIEUR") && !request.isUserInRole("ADMIN"))
            {
                throw new ProjectBusinessException("Action refusée : Seul un ingénieur technique peut uploader les documents de clôture.");
            }

            //basculement en phase post_projet
            if (projet.getPhase() == ProjectPhase.PROJET)
            {
                projet.setPhase(ProjectPhase.POST_PROJET);
                projectRepository.save(projet);

                //notif basculement du projet en phase POST_PROJET
                try {
                    //recup mail chef de projet lié au projet
                    java.util.Map<String, Object> cpMap = identityClient.getUserById(projet.getChefProjetId());
                    if (cpMap != null && cpMap.get("email") != null) {
                        String emailCP = cpMap.get("email").toString();
                        //envoi de la notif
                        notificationService.notifierLivrablesEtCloture(emailCP, projet.getTitre(), "L'équipe technique");
                    }
                } catch (Exception e) {
                    System.err.println("Erreur envoi notification bascule de phase : " + e.getMessage());
                }
            }
        }

        //phase= POST_PROJET
        if(projet.getPhase()==ProjectPhase.POST_PROJET && !request.isUserInRole("INGENIEUR"))
        {
            throw new ProjectBusinessException("Action refusée : Seul un ingénieur technique peut uploader en Post-projet.");
        }
        //creation de l'objet mongoDB
        ProjectDocument document = new ProjectDocument();
        document.setFileName(file.getOriginalFilename());
        document.setType(documentType);
        document.setProjectId(projectId); //lien avec le projet sur mysql
        document.setUploadDate(LocalDateTime.now());

        if (request.getUserPrincipal() != null) {
            document.setUploadedBy(request.getUserPrincipal().getName());
        }

        //on convertit le projet en binaire
        document.setData(file.getBytes());

        //on sauvegarde sur mongo
        ProjectDocument savedDocument = projectDocumentRepository.save(document);
        if (projet.getPhase() == ProjectPhase.PRE_PROJET) {
            try {
                java.util.Map<String, Object> cpMap = identityClient.getUserById(projet.getChefProjetId());
                if (cpMap != null && cpMap.get("email") != null) {
                    String emailCP = cpMap.get("email").toString();

                    String nomPresales = "L'ingénieur Presales";
                    if (request.getUserPrincipal() != null) {
                        nomPresales = request.getUserPrincipal().getName(); // Récupère l'email du token (ex: khadija@cis.sn)
                    }
                    // On utilise ta méthode existante dans NotificationService
                    notificationService.notifierNouveauPreProjet(emailCP, projet.getTitre(), nomPresales);
                }
            } catch (Exception e) {
                System.err.println("Erreur envoi alerte au Chef de Projet en phase Pré-projet : " + e.getMessage());
            }
        }

        if (documentType == DocumentType.PV_RECETTE || documentType == DocumentType.EXPLOITATION) {
            try {
                java.util.Map<String, Object> cpMap = identityClient.getUserById(projet.getChefProjetId());
                if (cpMap != null && cpMap.get("email") != null) {
                    String emailCP = cpMap.get("email").toString();
                    //envoi d'une alerte disant qu'un nouveau document final est disponible
                    notificationService.notifierLivrablesEtCloture(emailCP, projet.getTitre(), "L'ingénieur technique");
                }
            } catch (Exception e) {
                System.err.println("Erreur envoi alerte dépôt document : " + e.getMessage());
            }
        }

        return savedDocument;



    }*/

    //liste des documents d'un projet
    public List<ProjectDocument> getDocumentsByProject(Long projectId){
        return projectDocumentRepository.findByProjectId(projectId);
    }

    //recherche d'un document
    public ProjectDocument getDocument(String id){
        return projectDocumentRepository.findById(id).orElseThrow(()-> new ProjectBusinessException("Document introuvable!"));

    }

    public void deleteDocument(String documentId) {
        // 1. Récupérer le document dans MongoDB
        ProjectDocument document = projectDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ProjectBusinessException("Document introuvable !"));

        // 2. Récupérer l'utilisateur actuellement connecté
        String currentUser = "";
        if (request.getUserPrincipal() != null) {
            currentUser = request.getUserPrincipal().getName();
        }

        // 3. Vérification : Si ce n'est pas l'auteur ET que ce n'est pas un ADMIN -> On bloque
        if (!currentUser.equals(document.getUploadedBy()) && !request.isUserInRole("ADMIN")) {
            throw new ProjectBusinessException("Action refusée : Vous n'avez pas le droit de supprimer un document que vous n'avez pas téléversé.");
        }

        // 4. Si c'est bon, on supprime de MongoDB
        projectDocumentRepository.delete(document);
    }


    public List<DocumentDTO> getDocumentsByProjectId(Long projectId) {
        // 1. Récupère les documents du projet
        List<ProjectDocument> documents = projectDocumentRepository.findByProjectId(projectId);

        // 2. Transforme en DTO (sans le contenu binaire 'data')
        return documents.stream()
                .map(doc -> {
                    DocumentDTO dto = new DocumentDTO();
                    dto.setId(doc.getId()); // Ou String si tu as changé le DTO
                    dto.setFileName(doc.getFileName());
                    dto.setType(doc.getType());
                    dto.setUploadDate(doc.getUploadDate());
                    return dto;
                })
                .collect(Collectors.toList());
    }


}
