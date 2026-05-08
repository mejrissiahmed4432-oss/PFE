import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from '../toast.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div *ngFor="let toast of toasts" 
           class="toast-card" 
           [class]="toast.type"
           (click)="removeToast(toast.id)">
        
        <div class="toast-indicator"></div>
        
        <div class="toast-body">
          <div class="toast-icon-wrapper">
            <div class="icon-bg"></div>
            <i *ngIf="toast.type === 'success'" class="fas fa-check"></i>
            <i *ngIf="toast.type === 'info'" class="fas fa-info"></i>
            <i *ngIf="toast.type === 'error'" class="fas fa-exclamation-circle"></i>
            <i *ngIf="toast.type === 'warning'" class="fas fa-exclamation-triangle"></i>
          </div>
          
          <div class="toast-text">
            <div class="toast-header">
              <span class="toast-badge">{{ toast.type | uppercase }}</span>
              <span class="toast-time">Just now</span>
            </div>
            <p class="toast-message">{{ toast.message }}</p>
          </div>

          <button class="toast-close-btn">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="toast-progress-track">
          <div class="toast-progress-fill" [style.animation-duration.ms]="toast.duration || 5000"></div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 24px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 99999;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      pointer-events: none;
      width: 100%;
      padding: 0 20px;
    }

    .toast-card {
      pointer-events: auto;
      position: relative;
      width: 100%;
      max-width: 420px;
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(16px) saturate(200%);
      -webkit-backdrop-filter: blur(16px) saturate(200%);
      border: 1px solid rgba(255, 255, 255, 0.4);
      border-radius: 14px;
      box-shadow: 
        0 10px 15px -3px rgba(0, 0, 0, 0.08),
        0 4px 6px -2px rgba(0, 0, 0, 0.04),
        0 0 0 1px rgba(0, 0, 0, 0.02);
      overflow: hidden;
      cursor: pointer;
      animation: toastSlideIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }

    .toast-card:hover {
      transform: translateY(-2px);
      box-shadow: 
        0 20px 25px -5px rgba(0, 0, 0, 0.1),
        0 10px 10px -5px rgba(0, 0, 0, 0.04);
    }

    @keyframes toastSlideIn {
      from { transform: translateY(-40px) scale(0.95); opacity: 0; }
      to { transform: translateY(0) scale(1); opacity: 1; }
    }

    .toast-indicator {
      position: absolute;
      top: 0;
      left: 0;
      width: 4px;
      height: 100%;
      border-radius: 4px 0 0 4px;
    }

    .toast-body {
      display: flex;
      align-items: flex-start;
      padding: 16px 18px;
      gap: 14px;
    }

    .toast-icon-wrapper {
      position: relative;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .icon-bg {
      position: absolute;
      inset: 0;
      border-radius: 10px;
      opacity: 0.15;
    }

    .toast-icon-wrapper i {
      font-size: 1.1rem;
      position: relative;
      z-index: 1;
    }

    .toast-text {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
    }

    .toast-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }

    .toast-badge {
      font-size: 0.6rem;
      font-weight: 800;
      letter-spacing: 0.8px;
      padding: 2px 8px;
      border-radius: 6px;
      text-transform: uppercase;
    }

    .toast-time {
      font-size: 0.65rem;
      color: #94a3b8;
      font-weight: 500;
    }

    .toast-message {
      margin: 0;
      font-size: 0.92rem;
      font-weight: 600;
      color: #334155;
      line-height: 1.5;
      word-break: break-word;
    }

    .toast-close-btn {
      background: transparent;
      border: none;
      color: #cbd5e1;
      font-size: 0.85rem;
      padding: 4px;
      cursor: pointer;
      transition: all 0.2s;
      margin-top: -2px;
      margin-right: -4px;
    }

    .toast-close-btn:hover {
      color: #64748b;
      transform: rotate(90deg);
    }

    /* Type Variants */
    .toast-card.success .toast-indicator, 
    .toast-card.success .icon-bg { background: #10b981; }
    .toast-card.success i { color: #059669; }
    .toast-card.success .toast-badge { background: rgba(16, 185, 129, 0.1); color: #059669; }

    .toast-card.info .toast-indicator, 
    .toast-card.info .icon-bg { background: #3b82f6; }
    .toast-card.info i { color: #2563eb; }
    .toast-card.info .toast-badge { background: rgba(59, 130, 246, 0.1); color: #2563eb; }

    .toast-card.error .toast-indicator, 
    .toast-card.error .icon-bg { background: #ef4444; }
    .toast-card.error i { color: #dc2626; }
    .toast-card.error .toast-badge { background: rgba(239, 68, 68, 0.1); color: #dc2626; }

    .toast-card.warning .toast-indicator, 
    .toast-card.warning .icon-bg { background: #f59e0b; }
    .toast-card.warning i { color: #d97706; }
    .toast-card.warning .toast-badge { background: rgba(245, 158, 11, 0.1); color: #d97706; }

    /* Progress Bar */
    .toast-progress-track {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 2px;
      background: rgba(0, 0, 0, 0.03);
    }

    .toast-progress-fill {
      height: 100%;
      width: 100%;
      background: currentColor;
      opacity: 0.3;
      animation: shrink linear forwards;
    }

    .toast-card.success .toast-progress-fill { color: #10b981; }
    .toast-card.info .toast-progress-fill { color: #3b82f6; }
    .toast-card.error .toast-progress-fill { color: #ef4444; }
    .toast-card.warning .toast-progress-fill { color: #f59e0b; }

    @keyframes shrink {
      from { width: 100%; }
      to { width: 0%; }
    }

    /* Dark Mode */
    :host-context(.dark-mode) .toast-card {
      background: rgba(30, 41, 59, 0.9);
      border: 1px solid rgba(255, 255, 255, 0.08);
      box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.3);
    }

    :host-context(.dark-mode) .toast-message {
      color: #f1f5f9;
    }

    :host-context(.dark-mode) .toast-time {
      color: #64748b;
    }

    :host-context(.dark-mode) .toast-progress-track {
      background: rgba(255, 255, 255, 0.05);
    }
  `]
})
export class ToastComponent implements OnInit, OnDestroy {
  toasts: ToastMessage[] = [];
  private subscription?: Subscription;

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.subscription = this.toastService.toasts$.subscribe(toast => {
      this.toasts.push(toast);
      setTimeout(() => {
        this.removeToast(toast.id);
      }, toast.duration || 5000);
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  removeToast(id: number): void {
    this.toasts = this.toasts.filter(t => t.id !== id);
  }
}
