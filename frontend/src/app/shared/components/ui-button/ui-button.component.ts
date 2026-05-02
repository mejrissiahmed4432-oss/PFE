import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

@Component({
  selector: 'app-ui-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      [type]="type"
      [disabled]="disabled || loading"
      (click)="onClick.emit($event)"
      class="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium transition-all duration-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
      [ngClass]="[getVariantClasses(), fullWidth ? 'w-full' : '']">
      
      <!-- Loading Spinner -->
      <svg *ngIf="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>

      <!-- Custom Icon (if passed) -->
      <ng-content select="[icon]"></ng-content>
      
      <!-- Text content -->
      <ng-content></ng-content>
    </button>
  `,
  styles: ``
})
export class UiButtonComponent {
  @Input() variant: ButtonVariant = 'primary';
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() disabled: boolean = false;
  @Input() loading: boolean = false;
  @Input() fullWidth: boolean = false;
  @Output() onClick = new EventEmitter<MouseEvent>();

  getVariantClasses(): string {
    switch (this.variant) {
      case 'primary':
        return 'bg-blue-600 text-white hover:bg-blue-700 shadow-sm shadow-blue-500/30 focus:ring-blue-500';
      case 'secondary':
        return 'bg-gray-100 text-gray-700 hover:bg-gray-200 border border-transparent focus:ring-gray-500';
      case 'ghost':
        return 'bg-transparent text-gray-600 hover:bg-gray-100 focus:ring-gray-500';
      case 'danger':
        return 'bg-red-500 text-white hover:bg-red-600 shadow-sm shadow-red-500/30 focus:ring-red-500';
      default:
        return 'bg-blue-600 text-white hover:bg-blue-700 shadow-sm shadow-blue-500/30 focus:ring-blue-500';
    }
  }
}
