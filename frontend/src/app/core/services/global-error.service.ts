import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface GlobalErrorState {
  show: boolean;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class GlobalErrorService {
  private errorState = new BehaviorSubject<GlobalErrorState>({ show: false, message: '' });
  public errorState$ = this.errorState.asObservable();

  constructor() {}

  showError(message: string = 'Service is not available for now! Try later or contact Admin.') {
    this.errorState.next({ show: true, message });
  }

  hideError() {
    this.errorState.next({ show: false, message: '' });
  }
}
