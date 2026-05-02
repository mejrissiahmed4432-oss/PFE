import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="isOpen" class="relative z-50" aria-labelledby="modal-title" role="dialog" aria-modal="true">
      <!-- Background backdrop -->
      <div class="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity" (click)="close.emit()"></div>

      <div class="fixed inset-0 z-10 w-screen overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <!-- Modal panel -->
          <div class="relative transform overflow-hidden rounded-xl bg-white text-left shadow-2xl transition-all sm:my-8 sm:w-full"
               [ngClass]="maxWidthClass">
            
            <!-- Header -->
            <div class="bg-white px-4 py-4 sm:px-6 border-b border-gray-100 flex items-center justify-between">
              <h3 class="text-lg font-bold leading-6 text-gray-900" id="modal-title">{{ title }}</h3>
              <button type="button" class="text-gray-400 hover:text-gray-500 hover:bg-gray-100 rounded-lg p-1.5 transition-colors" (click)="close.emit()">
                <span class="sr-only">Close</span>
                <svg class="h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="bg-white px-4 py-5 sm:p-6">
              <ng-content></ng-content>
            </div>

            <!-- Footer (Optional) -->
            <div class="bg-gray-50 px-4 py-4 sm:flex sm:flex-row-reverse sm:px-6 border-t border-gray-100" *ngIf="showFooter">
              <ng-content select="[footer]"></ng-content>
            </div>

          </div>
        </div>
      </div>
    </div>
  `,
  styles: ``
})
export class UiModalComponent {
  @Input() isOpen: boolean = false;
  @Input() title: string = '';
  @Input() maxWidthClass: string = 'sm:max-w-lg'; // e.g. sm:max-w-sm, sm:max-w-2xl
  @Input() showFooter: boolean = true;
  @Output() close = new EventEmitter<void>();
}
