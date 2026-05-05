import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TabItem {
  id: string;
  label: string;
  icon?: string; // Optional SVG string or icon class if you add an icon system
  badge?: number;
}

@Component({
  selector: 'app-ui-tabs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="border-b border-gray-200">
      <nav class="-mb-px flex space-x-8" aria-label="Tabs">
        <button *ngFor="let tab of tabs"
           (click)="selectTab(tab.id)"
           [ngClass]="activeTabId === tab.id ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'"
           class="whitespace-nowrap flex py-4 px-1 border-b-2 font-medium text-sm transition-colors items-center gap-2">
          
          <!-- Optional Icon Slot -->
          <span *ngIf="tab.icon" [innerHTML]="tab.icon" class="w-5 h-5"></span>

          {{ tab.label }}
          
          <span *ngIf="tab.badge !== undefined" 
                [ngClass]="activeTabId === tab.id ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-900'"
                class="hidden ml-2 py-0.5 px-2.5 rounded-full text-xs font-medium md:inline-block transition-colors">
            {{ tab.badge }}
          </span>
        </button>
      </nav>
    </div>
  `,
  styles: ``
})
export class UiTabsComponent {
  @Input() tabs: TabItem[] = [];
  @Input() activeTabId: string = '';
  @Output() tabChange = new EventEmitter<string>();

  selectTab(id: string) {
    this.activeTabId = id;
    this.tabChange.emit(id);
  }
}
