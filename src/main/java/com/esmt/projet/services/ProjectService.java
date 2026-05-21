package com.esmt.projet.services;

import com.esmt.projet.client.IdentityClient;
import com.esmt.projet.dtos.ProjectRequestDTO;
import com.esmt.projet.dtos.ProjectResponseDTO;
import com.esmt.projet.dtos.TaskDTO;
import com.esmt.projet.entities.*;
import com.esmt.projet.exceptions.ProjectBusinessException;
import com.esmt.projet.repositories.mongodb.ProjectDocumentRepository;
import com.esmt.projet.repositories.jpa.ProjectRepository;
import com.esmt.projet.repositories.jpa.TaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectDocumentRepository documentRepository;
    private final IdentityClient  identityClient;
    private final HttpServletRequest   request;
    private final TaskRepository taskRepository;
    private final NotificationService  notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    //creer un projet
    @Transactional //sauvegarde echouee-> rien en base
    public ProjectResponseDTO creerProjet(ProjectRequestDTO request) {

        if (!identityClient.verifyUserRole(request.getPresalesId(), "PRESALES")) {
            throw new ProjectBusinessException("L'ID " + request.getPresalesId() + " n'est pas un membre de l'équipe PRESALES.");
        }

        if (!identityClient.verifyUserRole(request.getChefProjetId(), "CHEF_PROJET")) {
            throw new ProjectBusinessException("L'ID " + request.getChefProjetId() + " n'a pas les droits de CHEF_PROJET.");
        }

        if (!identityClient.verifyUserRole(request.getSuperviseurId(), "SUPERVISEUR")) {
            throw new ProjectBusinessException("L'ID " + request.getSuperviseurId() + " n'est pas un SUPERVISEUR valide.");
        }

        //on transforme le dto en entite
        Project project = new Project();
        project.setTitre(request.getTitre());
        project.setDescription(request.getDescription());
        project.setCategorie(request.getCategorie());
        project.setBudget(request.getBudget());
        project.setPresalesId(request.getPresalesId());
        project.setChefProjetId(request.getChefProjetId());
        project.setSuperviseurId(request.getSuperviseurId());
        project.setDateFinEstimee(request.getDateFinEstimee());

        //initialisation par defaut
        project.setPhase(ProjectPhase.PRE_PROJET); //un projet commence par cette phase
        project.setStatut(ProjectStatus.EN_ATTENTE); //en attente de documents pour les prerequis
        project.setAvancement(0); //0% par defaut
        project.setDateDebut(LocalDateTime.now()); //la date du moment lors de la creation du projet
        project.setDateFinEstimee(request.getDateFinEstimee());
        //sauvegarde dans mysql
        Project savedProject = projectRepository.save(project);

        //notifier le chef de projet
        try {
            //recup des infos du chef de projet
            Map<String, Object> cpMap = identityClient.getUserById(request.getChefProjetId());
            if (cpMap != null && cpMap.get("email") != null) {
                String emailCP = cpMap.get("email").toString();

                //envoi du mail
                notificationService.notifierNouveauPreProjet(
                        emailCP,
                        savedProject.getTitre(),
                        "L'équipe Presales"
                );
            }
        } catch (Exception e) {
            System.err.println("Échec notification création pré-projet : " + e.getMessage());
        }

        //Retourner la reponse cad retransformer l'entite en dto
        return mapToResponseDTO(savedProject);

    }

    //transformer l'entite en dto pour la reponse
    private ProjectResponseDTO mapToResponseDTO(Project project) {
        ProjectResponseDTO responseDTO = new ProjectResponseDTO();
        responseDTO.setId(project.getId());
        responseDTO.setTitre(project.getTitre());
        responseDTO.setPhase(project.getPhase());
        responseDTO.setStatut(project.getStatut());
        responseDTO.setAvancement(project.getAvancement());
        responseDTO.setDescription(project.getDescription());
        responseDTO.setCategorie(project.getCategorie());
        responseDTO.setBudget(project.getBudget());
        responseDTO.setPresalesId(project.getPresalesId());
        responseDTO.setChefProjetId(project.getChefProjetId());
        responseDTO.setDateDebut(project.getDateDebut());
        responseDTO.setDateFinEstimee(project.getDateFinEstimee());
        responseDTO.setSuperviseurId(project.getSuperviseurId());
        if (project.getTasks() != null) {
            List<TaskDTO> taskDTOs = project.getTasks().stream().map(task -> {
                TaskDTO dto = new TaskDTO();
                dto.setId(task.getId());
                dto.setIntitule(task.getIntitule());
                dto.setStatut(task.getStatut());
                dto.setIngenieurId(task.getIngenieurId());
                dto.setDateCreation(task.getDateCreation());
                return dto;
            }).toList();
            responseDTO.setTasks(taskDTOs);
        }

        return responseDTO;

    }

    public ProjectDocument getDocument(String id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ProjectBusinessException("Document non trouvé avec l'ID : " + id));
    }

    public ProjectResponseDTO validerPrerequisEtLancer(Long projectId) {

        //recuperer le projet
        Project projet = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Projet non trouvé"));

        // seul le chef de projet peut valider les prerequis dans la phase preprojet
        if(!request.isUserInRole("CHEF_PROJET")) {
            throw new ProjectBusinessException("Action refusée : Seul le Chef de Projet peut valider les prérequis.");
        }

        //verifier la phase--> est bien PRE_PROJET???
        if (projet.getPhase() != ProjectPhase.PRE_PROJET)
        {
            throw new ProjectBusinessException("Le projet est déjà entré en phase d'exécution ou finale.");
        }

        //validation--> changement de phase
        projet.setPhase(ProjectPhase.PROJET);
        projet.setStatut(ProjectStatus.EN_COURS);

        //sauvegarde
        Project updatedProject = projectRepository.save(projet);
        return mapToResponseDTO(updatedProject);
    }

    //ajouter une tache à un projet
    public ProjectResponseDTO ajouterTask(Long projectId, TaskDTO taskDTO) {
        Project projet=projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Projet non trouvé"));

        //verification de la phase : l'ajout d'une tache se fait en phase PROJET
        if(projet.getPhase() != ProjectPhase.PROJET)
        {
            throw new ProjectBusinessException("Action impossible : Le projet n'est pas en phase active (PROJET).");
        }
        Task task = new Task();
        task.setIntitule(taskDTO.getIntitule());
        task.setIngenieurId(taskDTO.getIngenieurId());
        task.setStatut(TaskStatus.A_FAIRE);
        task.setDateCreation(taskDTO.getDateCreation());
        task.setProject(projet);

        taskRepository.save(task);

        //refresh pour la liste de taches
        entityManager.flush();
        entityManager.refresh(projet);

        //recalcul automatique de l'avancement du projet KPI
        recalculerAvancement(projet);

        //notification a l'ingénieur assigné
        try {
            //recup du user
            Map<String, Object> userMap = identityClient.getUserById(taskDTO.getIngenieurId());

            if (userMap != null && userMap.get("email") != null) {
                String emailIngenieur = userMap.get("email").toString();

                //envoi de l'e-mail d'assignation
                notificationService.notifierAssignationTache(
                        emailIngenieur,
                        projet.getTitre(),
                        task.getIntitule()
                );
            }
        } catch (Exception e) {
            System.err.println("Alerte email non envoyée à l'ingénieur : " + e.getMessage());
        }

        return mapToResponseDTO(projectRepository.save(projet));
    }

    //modifier le statut d'une tache : A_FAIRE? EN_COURS? TERMINE??
    @Transactional
    public ProjectResponseDTO changerStatutTask(Long taskId, TaskStatus newStatut) {
        Task task= taskRepository.findById(taskId)
                .orElseThrow(() -> new ProjectBusinessException("Tâche non trouvée"));
        task.setStatut(newStatut);
        taskRepository.save(task);

        //recalcul automatique de l'avancement du projet lié
        Project projet = task.getProject();

        //refrewh
        entityManager.flush();
        entityManager.refresh(projet);
        recalculerAvancement(projet);
        return mapToResponseDTO(projectRepository.save(projet));
    }

    //mettre à jour le KPI d'avancement (0 à 100%)
    private void recalculerAvancement(Project project) {
        List<Task> tasks = project.getTasks();
        int totalTasks = tasks.size();
        if(tasks == null || tasks.isEmpty())
        {
            project.setAvancement(0);
            return;
        }
        long tachesTerminees = tasks.stream()
                .filter(t -> t.getStatut() == TaskStatus.TERMINE)
                .count();
        int pourcentage = (int) ((tachesTerminees * 100)/totalTasks);
        project.setAvancement(pourcentage);
        if (pourcentage == 100 && project.getPhase() == ProjectPhase.PROJET )
        {
            //changement phase et statut
            project.setPhase(ProjectPhase.POST_PROJET);
            project.setStatut(ProjectStatus.EN_COURS);

            //envoi notif chef projet
            try {
                Map<String, Object> cpMap = identityClient.getUserById(project.getChefProjetId());
                if (cpMap != null && cpMap.get("email") != null) {
                    notificationService.notifierLivrablesEtCloture(
                            cpMap.get("email").toString(),
                            project.getTitre(),
                            "Le système d'avancement"
                    );
                }
            } catch (Exception e) {
                System.err.println("Échec alerte bascule automatique : " + e.getMessage());
            }
        }
    }

    //cloturer le projet
    @Transactional
    public ProjectResponseDTO cloturerProjet(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Projet non trouvé"));

        //seul le Chef de Projet ou l'Admin peut clore
        if (!request.isUserInRole("CHEF_PROJET") && !request.isUserInRole("ADMIN")) {
            throw new ProjectBusinessException("Action refusée : Seul le Chef de Projet peut clôturer l'exécution.");
        }

        //les taches doivent toutes etre a 100%
        if (project.getAvancement() < 100) {
            throw new ProjectBusinessException("Action impossible : L'avancement doit être à 100% pour passer en Post-Projet.");
        }

        //passage à la phase post_projet
        project.setPhase(ProjectPhase.POST_PROJET);
        project.setStatut(ProjectStatus.TERMINE);

        Project updatedProject = projectRepository.save(project);
        return mapToResponseDTO(updatedProject);
    }


    //exportation excel des kpi et infos sur l'avancement des projets

    public byte[] exporterStatistiquesProjets2026() throws Exception {
        //recuperation de la liste des projets depuis mysql
        List<Project> projets = projectRepository.findAll();

        //creation d'un classeur excel
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            //creation d'un onglet Suivi de projets
            Sheet sheet = workbook.createSheet("Suivi Projets");

            //on crée la 1e ligne --> titres des colonnes (index 0)
            Row headerRow = sheet.createRow(0);
            String[] colonnes = {"ID", "Titre du Projet", "Catégorie", "Phase", "Statut", "Avancement (%)"};

            //boucle for pour remplir cette 1e ligne
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
            }

            //on remplit le reste des lignes avec les projets de la bd
            int numeroLigne = 1; //on commence par la ligne1

            for (Project p : projets) {
                Row row = sheet.createRow(numeroLigne++); //crée une nouvelle ligne et passe à la suivante

                //on remplit chaque case de la ligne avec les infos du projet p
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getTitre());
                row.createCell(2).setCellValue(p.getCategorie().toString());
                row.createCell(3).setCellValue(p.getPhase().toString());
                row.createCell(4).setCellValue(p.getStatut().toString());
                row.createCell(5).setCellValue(p.getAvancement());
            }

            //on transforme le classeur en binaire (tableau d'octets)
            workbook.write(out);
            return out.toByteArray();
        }
    }

}
