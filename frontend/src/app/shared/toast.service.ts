import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ToastMessage {
  id: number;
  message: string;
  type: 'success' | 'info' | 'error' | 'warning' | 'delete' | 'update';
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastSubject = new Subject<ToastMessage>();
  toasts$ = this.toastSubject.asObservable();

  show(message: string, type: 'success' | 'info' | 'error' | 'warning' | 'delete' | 'update' = 'success', duration: number = 5000): void {
    const id = Date.now();
    this.toastSubject.next({ id, message, type, duration });
  }

  success(message: string, duration?: number): void {
    this.show(message, 'success', duration);
  }

  info(message: string, duration?: number): void {
    this.show(message, 'info', duration);
  }

  error(message: string, duration?: number): void {
    this.show(message, 'error', duration);
  }

  warning(message: string, duration?: number): void {
    this.show(message, 'warning', duration);
  }

  delete(message: string, duration?: number): void {
    this.show(message, 'delete', duration);
  }

  update(message: string, duration?: number): void {
    this.show(message, 'update', duration);
  }
}
