import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RefreshService {
  private refreshSubject = new Subject<string>();

  // Observable for components to subscribe to
  refresh$ = this.refreshSubject.asObservable();

  /**
   * Triggers a refresh event.
   * @param actionType The type of action that triggered the refresh (optional)
   */
  triggerRefresh(actionType: string = 'GENERAL'): void {
    console.log(`[RefreshService] Triggering refresh for action: ${actionType}`);
    this.refreshSubject.next(actionType);
  }
}
