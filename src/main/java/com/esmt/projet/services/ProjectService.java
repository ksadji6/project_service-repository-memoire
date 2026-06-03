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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectDocumentRepository documentRepository;
    private final IdentityClient identityClient;
    private final HttpServletRequest request;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    // creer un projet
    @Transactional
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

        Project project = new Project();
        project.setTitre(request.getTitre());
        project.setDescription(request.getDescription());
        project.setCategorie(request.getCategorie());
        project.setBudget(request.getBudget());
        project.setPresalesId(request.getPresalesId());
        project.setChefProjetId(request.getChefProjetId());
        project.setSuperviseurId(request.getSuperviseurId());
        project.setDateFinEstimee(request.getDateFinEstimee());

        project.setPhase(ProjectPhase.PRE_PROJET);
        project.setStatut(ProjectStatus.EN_ATTENTE);
        project.setAvancement(0);
        project.setDateDebut(LocalDateTime.now());

        Project savedProject = projectRepository.save(project);

        try {
            Map<String, Object> cpMap = identityClient.getUserById(request.getChefProjetId());
            if (cpMap != null && cpMap.get("email") != null) {
                String emailCP = cpMap.get("email").toString();
                notificationService.notifierNouveauPreProjet(emailCP, savedProject.getTitre(), "L'équipe Presales");
            }
        } catch (Exception e) {
            System.err.println("Échec notification création pré-projet : " + e.getMessage());
        }

        return mapToResponseDTO(savedProject);
    }

    // transformer l'entite en dto pour la reponse
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
        Project projet = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Projet non trouvé"));

        if(!request.isUserInRole("CHEF_PROJET")) {
            throw new ProjectBusinessException("Action refusée : Seul le Chef de Projet peut valider les prérequis.");
        }
        if (projet.getPhase() != ProjectPhase.PRE_PROJET) {
            throw new ProjectBusinessException("Le projet est déjà entré en phase d'exécution ou finale.");
        }

        projet.setPhase(ProjectPhase.PROJET);
        projet.setStatut(ProjectStatus.EN_COURS);

        Project updatedProject = projectRepository.save(projet);
        return mapToResponseDTO(updatedProject);
    }

    public ProjectResponseDTO rechercherProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Projet non trouvé avec l'ID : " + projectId));
        return mapToResponseDTO(project);
    }

    // 🎯 recuperer tous les projets de la bdd pour ton front
    public List<ProjectResponseDTO> rechercherTousLesProjets() {
        List<Project> projets = projectRepository.findAll();
        return projets.stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    // ajouter une tache a un projet
    @Transactional
    public ProjectResponseDTO ajouterTask(Long projectId, TaskDTO taskDTO) {
        Project projet = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Projet non trouvé"));

        if(projet.getPhase() != ProjectPhase.PROJET) {
            throw new ProjectBusinessException("Action impossible : Le projet n'est pas en phase active.");
        }

        Task task = new Task();
        task.setIntitule(taskDTO.getIntitule());
        task.setIngenieurId(taskDTO.getIngenieurId());
        task.setStatut(TaskStatus.A_FAIRE);
        task.setDateCreation(taskDTO.getDateCreation());
        task.setProject(projet);

        // 1. Sauvegarde la tâche
        taskRepository.save(task);

        // 2. Pas besoin de flush/refresh.
        // Spring Data gère la synchronisation automatiquement à la fin de la méthode @Transactional.

        // 3. Recalculer l'avancement (assure-toi que la liste des tâches dans l'objet 'projet' est mise à jour)
        if (projet.getTasks() == null) {
            projet.setTasks(new ArrayList<>());
        }
        projet.getTasks().add(task);
        recalculerAvancement(projet);

        // 4. Sauvegarde le projet mis à jour
        Project savedProject = projectRepository.save(projet);

        // ... (ton code de notification reste identique) ...

        return mapToResponseDTO(savedProject);
    }
    /*public ProjectResponseDTO ajouterTask(Long projectId, TaskDTO taskDTO) {
        Project projet = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Projet non trouvé"));

        if(projet.getPhase() != ProjectPhase.PROJET) {
            throw new ProjectBusinessException("Action impossible : Le projet n'est pas en phase active (PROJET).");
        }

        Task task = new Task();
        task.setIntitule(taskDTO.getIntitule());
        task.setIngenieurId(taskDTO.getIngenieurId());
        task.setStatut(TaskStatus.A_FAIRE);
        task.setDateCreation(taskDTO.getDateCreation());
        task.setProject(projet);

        taskRepository.save(task);

        entityManager.flush();
        entityManager.refresh(projet);
        recalculerAvancement(projet);

        try {
            Map<String, Object> userMap = identityClient.getUserById(taskDTO.getIngenieurId());
            if (userMap != null && userMap.get("email") != null) {
                String emailIngenieur = userMap.get("email").toString();
                notificationService.notifierAssignationTache(emailIngenieur, projet.getTitre(), task.getIntitule());
            }
        } catch (Exception e) {
            System.err.println("Alerte email non envoyée à l'ingénieur : " + e.getMessage());
        }

        return mapToResponseDTO(projectRepository.save(projet));
    }*/

    // modifier le statut d'une tache
    @Transactional
    public ProjectResponseDTO changerStatutTask(Long taskId, TaskStatus newStatut) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ProjectBusinessException("Tâche non trouvée"));
        task.setStatut(newStatut);
        taskRepository.save(task);

        Project projet = task.getProject();
        entityManager.flush();
        entityManager.refresh(projet);
        recalculerAvancement(projet);
        return mapToResponseDTO(projectRepository.save(projet));
    }

    // mettre a jour le kpi d'avancement
    private void recalculerAvancement(Project project) {
        List<Task> tasks = project.getTasks();
        int totalTasks = tasks.size();
        if(tasks == null || tasks.isEmpty()) {
            project.setAvancement(0);
            return;
        }
        long tachesTerminees = tasks.stream()
                .filter(t -> t.getStatut() == TaskStatus.TERMINE)
                .count();
        int pourcentage = (int) ((tachesTerminees * 100)/totalTasks);
        project.setAvancement(pourcentage);

        if (pourcentage == 100 && project.getPhase() == ProjectPhase.PROJET ) {
            project.setPhase(ProjectPhase.POST_PROJET);
            project.setStatut(ProjectStatus.EN_COURS);

            try {
                Map<String, Object> cpMap = identityClient.getUserById(project.getChefProjetId());
                if (cpMap != null && cpMap.get("email") != null) {
                    notificationService.notifierLivrablesEtCloture(cpMap.get("email").toString(), project.getTitre(), "Le système d'avancement");
                }
            } catch (Exception e) {
                System.err.println("Échec alerte bascule automatique : " + e.getMessage());
            }
        }
    }

    // cloturer le projet
    @Transactional
    public ProjectResponseDTO cloturerProjet(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectBusinessException("Projet non trouvé"));

        if (!request.isUserInRole("CHEF_PROJET") && !request.isUserInRole("ADMIN")) {
            throw new ProjectBusinessException("Action refusée : Seul le Chef de Projet peut clôturer l'exécution.");
        }
        if (project.getAvancement() < 100) {
            throw new ProjectBusinessException("Action impossible : L'avancement doit être à 100% pour passer en Post-Projet.");
        }

        project.setPhase(ProjectPhase.POST_PROJET);
        project.setStatut(ProjectStatus.TERMINE);

        Project updatedProject = projectRepository.save(project);
        return mapToResponseDTO(updatedProject);
    }

    // exportation excel des kpi
    public byte[] exporterStatistiquesProjets2026() throws Exception {
        List<Project> projets = projectRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Suivi Projets");
            Row headerRow = sheet.createRow(0);
            String[] colonnes = {"ID", "Titre du Projet", "Catégorie", "Phase", "Statut", "Avancement (%)"};

            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
            }

            int numeroLigne = 1;
            for (Project p : projets) {
                Row row = sheet.createRow(numeroLigne++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getTitre());
                row.createCell(2).setCellValue(p.getCategorie().toString());
                row.createCell(3).setCellValue(p.getPhase().toString());
                row.createCell(4).setCellValue(p.getStatut().toString());
                row.createCell(5).setCellValue(p.getAvancement());
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}