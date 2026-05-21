import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ConfirmDialogConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDanger?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ConfirmDialogService {
  private dialogSubject = new Subject<ConfirmDialogConfig>();
  dialogState$ = this.dialogSubject.asObservable();

  private resolveResponse!: (value: boolean) => void;

  confirm(config: ConfirmDialogConfig): Promise<boolean> {
    this.dialogSubject.next(config);
    return new Promise(resolve => {
      this.resolveResponse = resolve;
    });
  }

  respond(result: boolean): void {
    if (this.resolveResponse) {
      this.resolveResponse(result);
    }
  }
}
