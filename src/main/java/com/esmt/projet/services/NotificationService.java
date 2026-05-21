package com.esmt.projet.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    //phase= PRE_PROJET --> notif au chef projet quand le presales uload les documents
    public void notifierNouveauPreProjet(String emailChefProjet, String titreProjet, String nomPresales) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(emailChefProjet);
        mailMessage.setSubject(" Nouveau dossier Pré-projet à valider - CIS Integration");
        mailMessage.setText("Bonjour,\n\n" +
                "L'ingénieur Presales (" + nomPresales + ") vient de charger les documents d'opportunité " +
                "pour le projet suivant : \"" + titreProjet + "\".\n\n" +
                "Le projet est actuellement en phase PRE_PROJET. Veuillez vérifier et valider les prérequis " +
                "techniques pour lancer la phase d'exécution.\n\n" +
                "Cordialement,\n" +
                "Système de Notification CIS.");
        mailSender.send(mailMessage);
    }

    //phase= PROJET --> notif a chaque assignation de tache
    public void notifierAssignationTache(String emailIngenieur, String titreProjet, String intituleTache) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(emailIngenieur);
        mailMessage.setSubject("Nouvelle tâche assignée - Projet : " + titreProjet);
        mailMessage.setText("Bonjour,\n\n" +
                "Le Chef de Projet vient de vous assigner une nouvelle tâche technique.\n\n" +
                "Projet : " + titreProjet + "\n" +
                "Tâche à réaliser : " + intituleTache + "\n" +
                "Statut initial : A_FAIRE\n\n" +
                "Veuillez vous connecter à la plateforme CIS pour consulter les détails et mettre à jour son avancement au fur et à mesure.\n\n" +
                "Cordialement,\n" +
                "Plateforme de Suivi des Projets CIS.");
        mailSender.send(mailMessage);
    }

    //phase=POST_PROJET --> notif au chef de projet après l'upload des livrables pour la cloture du projet
    public void notifierLivrablesEtCloture(String emailChefProjet, String titreProjet, String nomIngenieur) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(emailChefProjet);
        mailMessage.setSubject("Livrables de clôture disponibles - " + titreProjet);
        mailMessage.setText("Bonjour,\n\n" +
                "L'ingénieur technique (" + nomIngenieur + ") a téléversé avec succès les documents de fin d'exécution " +
                "Le projet \"" + titreProjet + "\" a atteint 100% d'avancement technique et est passé " +
                "automatiquement en phase POST_PROJET.\n\n" +
                "Le dossier est prêt pour la validation finale et la clôture administrative.\n\n" +
                "Cordialement,\n" +
                "Service de Gestion Documentaire CIS.");
        mailSender.send(mailMessage);
    }
}