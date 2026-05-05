import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css'
})
export class TopbarComponent {
  @Input() pageTitle: string = 'Dashboard';
  @Input() user: any;
  @Input() unreadMessagesCount: number = 0;
  @Input() unreadNotificationsCount: number = 0;
  @Input() unreadAlertsCount: number = 0;
  @Input() isNotificationsOpen: boolean = false;
  @Input() notificationsList: any[] = [];
  @Input() selectedLanguage: string = 'en';

  @Output() toggleAssistant = new EventEmitter<void>();
  @Output() openMessages = new EventEmitter<void>();
  @Output() toggleNotifications = new EventEmitter<MouseEvent>();
  @Output() openAlerts = new EventEmitter<void>();
  @Output() changeLanguage = new EventEmitter<string>();
  @Output() logout = new EventEmitter<void>();
  @Output() openProfile = new EventEmitter<void>();
  @Output() markAllNotificationsAsRead = new EventEmitter<MouseEvent>();
  @Output() deleteNotification = new EventEmitter<{id: string, event: MouseEvent}>();
  @Output() deleteAllNotifications = new EventEmitter<MouseEvent>();
}
