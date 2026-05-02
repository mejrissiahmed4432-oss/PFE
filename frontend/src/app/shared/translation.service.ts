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
