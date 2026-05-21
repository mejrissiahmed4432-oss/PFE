import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmDialogService, ConfirmDialogConfig } from './confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="config" class="fixed inset-0 z-[9999] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-gray-100 overflow-hidden transform transition-all">
        <!-- Header -->
        <div class="px-6 py-5 border-b border-gray-100" [ngClass]="{'bg-red-50': config.isDanger}">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0" [ngClass]="config.isDanger ? 'bg-red-100 text-red-600' : 'bg-indigo-100 text-indigo-600'">
              <svg *ngIf="config.isDanger" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              <svg *ngIf="!config.isDanger" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            </div>
            <h3 class="text-lg font-bold" [ngClass]="config.isDanger ? 'text-red-900' : 'text-gray-900'">{{ config.title }}</h3>
          </div>
        </div>
        
        <!-- Body -->
        <div class="px-6 py-6 text-gray-600 text-sm leading-relaxed">
          {{ config.message }}
        </div>
        
        <!-- Footer -->
        <div class="px-6 py-4 bg-gray-50 border-t border-gray-100 flex items-center justify-end gap-3">
          <button class="px-5 py-2.5 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-200 transition-colors" (click)="onCancel()">
            {{ config.cancelText || 'Cancel' }}
          </button>
          <button class="px-5 py-2.5 rounded-xl text-sm font-bold text-white transition-all"
                  [ngClass]="config.isDanger ? 'bg-red-600 hover:bg-red-700 shadow-lg shadow-red-200' : 'bg-indigo-600 hover:bg-indigo-700 shadow-lg shadow-indigo-200'"
                  (click)="onConfirm()">
            {{ config.confirmText || 'Confirm' }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class ConfirmDialogComponent implements OnInit {
  config: ConfirmDialogConfig | null = null;

  constructor(private confirmDialogService: ConfirmDialogService) {}

  ngOnInit(): void {
    this.confirmDialogService.dialogState$.subscribe(config => {
      this.config = config;
    });
  }

  onConfirm(): void {
    this.confirmDialogService.respond(true);
    this.config = null;
  }

  onCancel(): void {
    this.confirmDialogService.respond(false);
    this.config = null;
  }
}
