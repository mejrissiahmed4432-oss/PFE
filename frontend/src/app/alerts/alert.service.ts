import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Alert {
  id: string;
  title: string;
  message: string;
  type: 'WARNING' | 'INFO' | 'ERROR' | 'SUCCESS';
  category: string;
  read: boolean;
  relatedId?: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private apiUrl = '/api/alerts';

  constructor(private http: HttpClient) { }

  getAlerts(): Observable<Alert[]> {
    return this.http.get<Alert[]>(this.apiUrl);
  }

  getUnreadAlerts(): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.apiUrl}/unread`);
  }

  markAsRead(id: string): Observable<Alert> {
    return this.http.put<Alert>(`${this.apiUrl}/${id}/read`, {});
  }

  generateTestAlerts(): Observable<any> {
    return this.http.post(`${this.apiUrl}/generate-test`, {});
  }
}
