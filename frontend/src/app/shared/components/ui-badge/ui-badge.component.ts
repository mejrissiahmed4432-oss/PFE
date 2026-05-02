import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'default' | 'primary' | 'neutral';

@Component({
  selector: 'app-ui-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span 
      class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold uppercase tracking-wider"
      [ngClass]="[getVariantClasses(), getSizeClasses()]">
      <ng-content></ng-content>
    </span>
  `,
  styles: ``
})
export class UiBadgeComponent {
  @Input() variant: BadgeVariant = 'default';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';

  getVariantClasses(): string {
    switch (this.variant) {
      case 'success':
        return 'bg-green-100 text-green-800 border border-green-200';
      case 'warning':
        return 'bg-amber-100 text-amber-800 border border-amber-200';
      case 'danger':
        return 'bg-red-100 text-red-800 border border-red-200';
      case 'info':
      case 'primary':
        return 'bg-blue-100 text-blue-800 border border-blue-200';
      case 'default':
      case 'neutral':
      default:
        return 'bg-gray-100 text-gray-800 border border-gray-200';
    }
  }
  
  getSizeClasses(): string {
    switch (this.size) {
      case 'sm': return 'text-[8px] px-1.5 py-0';
      case 'lg': return 'text-sm px-4 py-1.5';
      case 'md':
      default: return 'text-xs px-2.5 py-0.5';
    }
  }
}
