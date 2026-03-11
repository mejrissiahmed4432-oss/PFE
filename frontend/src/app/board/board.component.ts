import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';
import { AiAssistantComponent } from '../ai-assistant/ai-assistant.component';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule, AiAssistantComponent],
  templateUrl: './board.component.html',
  styleUrl: './board.component.css'
})
export class BoardComponent implements OnInit {
  user: any;
  selectedLanguage: 'en' | 'fr' = 'en'; // Default is English
  isAssistantOpen: boolean = false;

  constructor(private authService: AuthService, private router: Router) { }

  ngOnInit(): void {
    this.user = this.authService.getCurrentUser();
    if (!this.user) {
      this.router.navigate(['/login']);
    }
  }

  selectLanguage(lang: 'en' | 'fr'): void {
    this.selectedLanguage = lang;
  }

  toggleAssistant(): void {
    this.isAssistantOpen = !this.isAssistantOpen;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
