import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  @Input() user: any;
  @Input() isSidebarCollapsed: boolean = false;
  @Input() activeTab: string = 'dashboard';
  @Input() selectedNatureFilter: string = '';
  @Input() selectedResourceFilter: string = '';

  @Output() toggleSidebar = new EventEmitter<void>();
  @Output() tabChange = new EventEmitter<string>();
  @Output() natureFilterChange = new EventEmitter<string>();
  @Output() resourceFilterChange = new EventEmitter<string>();

  onToggleSidebar() {
    this.toggleSidebar.emit();
  }

  onTabChange(tab: string) {
    this.tabChange.emit(tab);
  }

  onNatureFilterChange(filter: string) {
    this.natureFilterChange.emit(filter);
  }

  onResourceFilterChange(filter: string) {
    this.resourceFilterChange.emit(filter);
  }
}
