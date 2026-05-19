import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private currentLang = new BehaviorSubject<'en' | 'fr'>('en');
  currentLang$ = this.currentLang.asObservable();

  private translations: any = {
    en: {
      'Dashboard': 'Dashboard',
      'Inventory': 'Inventory',
      'Assets': 'Assets',
      'Consumables': 'Consumables',
      'Categories': 'Categories',
      'Suppliers': 'Suppliers',
      'Orders': 'Orders',
      'Stock': 'Stock',
      'Shelves': 'Shelves',
      'Management': 'Management',
      'Resources': 'Resources',
      'Tickets': 'Support Tickets',
      'Schedule': 'Schedule & Tasks',
      'Reports': 'Reports',
      'Messages': 'Messages',
      'Settings': 'Account Settings',
      'Profile': 'My Profile',
      'AI_Insights': 'AI Insights',
      'Notifications': 'Notifications',
      'MedinaFlux_Platform': 'MedinaFlux Platform',
      'Switch_Language': 'Switch Language',
      'Light_Mode': 'Switch to Light Mode',
      'Dark_Mode': 'Switch to Dark Mode',

      'Service_Desk': 'Service Desk',
      'Equipment_Ticket_Management': 'Equipment & Ticket Management',
      'Equipment_Browser': 'Equipment Browser',
      'Service_Queue': 'Service Queue',
      'My_Tickets': 'My Tickets',
      'Filters': 'Filters',
      
      // Ticket & Service Desk specific keys
      'Active_Tickets': 'Active Tickets',
      'Completed_Tickets': 'Completed Tickets',
      'Create_Ticket': 'Create Ticket',
      'New_Ticket': 'New Support Ticket',
      'Cancel': 'Cancel',
      'Confirm': 'Confirm',
      'Status': 'Status',
      'Priority': 'Priority',
      'Low': 'Low',
      'Medium': 'Medium',
      'High': 'High',
      'Open': 'Open',
      'In_Progress': 'In Progress',
      'Waiting': 'Waiting',
      'Testing': 'Testing',
      'Completed': 'Completed',
      'Title': 'Title',
      'Description': 'Description',
      'Equipment': 'Equipment',
      'Select_Equipment': 'Select Equipment',
      'Category': 'Category',
      'Select_Category': 'Select Category',
      'Actions': 'Actions',
      'Edit': 'Edit',
      'Delete': 'Delete',
      'Save': 'Save',
      'Workbench': 'Live Workbench',
      'Diagnosis': 'Diagnosis',
      'Plan': 'Maintenance Plan',
      'Resources_Sync': 'Resource Sync',
      'Execution': 'Execution',
      'Validation': 'Validation',
      'Summary': 'Summary',
      'No_tickets_found': 'No tickets found',
      'Assigned_To_Me': 'Assigned to Me',
      'All_Tickets': 'All Tickets',
      'Submit': 'Submit',
      'Update': 'Update',
      'Equipment_List': 'Equipment List',
      'Search_Equipment': 'Search equipment...',
      'View_All_Equipment': 'View All Equipment',
      'Viewing_Equipment_In': 'Viewing equipment contained in',
      'Shelf': 'Shelf',
      'Search_Tickets': 'Search tickets...',
      'Support_Tickets': 'Support Tickets',
      'Asset_Inventory': 'Asset Inventory',
      'Details': 'Details',
      'History': 'History',
      'Specifications': 'Specifications',
      'Add_Note': 'Add Note',
      'Mark_as_Broken': 'Mark as Broken',
      'Reopen_Ticket': 'Reopen Ticket',
      'Delete_Warning': 'Are you sure you want to delete this?',
      'Good_Morning': 'Good morning',
      'Inventory_Status': 'Here is what is happening with your inventory today.'
    },
    fr: {
      'Dashboard': 'Tableau de bord',
      'Inventory': 'Inventaire',
      'Assets': 'Actifs',
      'Consumables': 'Consommables',
      'Categories': 'Catégories',
      'Suppliers': 'Fournisseurs',
      'Orders': 'Commandes',
      'Stock': 'Stock',
      'Shelves': 'Étagères',
      'Management': 'Gestion',
      'Resources': 'Ressources',
      'Tickets': 'Tickets de support',
      'Schedule': 'Calendrier et Tâches',
      'Reports': 'Rapports',
      'Messages': 'Messages',
      'Settings': 'Paramètres du compte',
      'Profile': 'Mon Profil',
      'AI_Insights': 'AI Insights',
      'Notifications': 'Notifications',
      'MedinaFlux_Platform': 'Plateforme MedinaFlux',
      'Switch_Language': 'Changer de langue',
      'Light_Mode': 'Passer au mode clair',
      'Dark_Mode': 'Passer au mode sombre',

      'Service_Desk': 'Centre de Services',
      'Equipment_Ticket_Management': 'Gestion des Équipements et Tickets',
      'Equipment_Browser': 'Navigateur d\'Équipement',
      'Service_Queue': 'File d\'Attente',
      'My_Tickets': 'Mes Tickets',
      'Filters': 'Filtres',
      
      // Ticket & Service Desk specific keys
      'Active_Tickets': 'Tickets Actifs',
      'Completed_Tickets': 'Tickets Terminés',
      'Create_Ticket': 'Créer un Ticket',
      'New_Ticket': 'Nouveau Ticket de Support',
      'Cancel': 'Annuler',
      'Confirm': 'Confirmer',
      'Status': 'Statut',
      'Priority': 'Priorité',
      'Low': 'Basse',
      'Medium': 'Moyenne',
      'High': 'Haute',
      'Open': 'Ouvert',
      'In_Progress': 'En Cours',
      'Waiting': 'En Attente',
      'Testing': 'En Test',
      'Completed': 'Terminé',
      'Title': 'Titre',
      'Description': 'Description',
      'Equipment': 'Équipement',
      'Select_Equipment': 'Sélectionner l\'Équipement',
      'Category': 'Catégorie',
      'Select_Category': 'Sélectionner la Catégorie',
      'Actions': 'Actions',
      'Edit': 'Modifier',
      'Delete': 'Supprimer',
      'Save': 'Enregistrer',
      'Workbench': 'Établi en Direct',
      'Diagnosis': 'Diagnostic',
      'Plan': 'Plan de Maintenance',
      'Resources_Sync': 'Synchronisation',
      'Execution': 'Exécution',
      'Validation': 'Validation',
      'Summary': 'Résumé',
      'No_tickets_found': 'Aucun ticket trouvé',
      'Assigned_To_Me': 'Assigné à Moi',
      'All_Tickets': 'Tous les Tickets',
      'Submit': 'Soumettre',
      'Update': 'Mettre à Jour',
      'Equipment_List': 'Liste des Équipements',
      'Search_Equipment': 'Rechercher un Équipement...',
      'View_All_Equipment': 'Voir tous les Équipements',
      'Viewing_Equipment_In': 'Affichage des Équipements dans',
      'Shelf': 'Étagère',
      'Search_Tickets': 'Rechercher des Tickets...',
      'Support_Tickets': 'Tickets de Support',
      'Asset_Inventory': 'Inventaire des Actifs',
      'Details': 'Détails',
      'History': 'Historique',
      'Specifications': 'Spécifications',
      'Add_Note': 'Ajouter une Note',
      'Mark_as_Broken': 'Marquer comme Cassé',
      'Reopen_Ticket': 'Rouvrir le Ticket',
      'Delete_Warning': 'Êtes-vous sûr de vouloir supprimer ceci ?',
      'Good_Morning': 'Bonjour',
      'Inventory_Status': 'Voici ce qui se passe avec votre inventaire aujourd\'hui.'
    }
  };

  setLanguage(lang: 'en' | 'fr') {
    this.currentLang.next(lang);
    localStorage.setItem('lang', lang);
  }

  getLanguage() {
    return this.currentLang.value;
  }

  translate(key: string): string {
    const lang = this.currentLang.value;
    return this.translations[lang][key] || key;
  }

  constructor() {
    const savedLang = localStorage.getItem('lang') as 'en' | 'fr';
    if (savedLang) {
      this.currentLang.next(savedLang);
    }
  }
}
