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

  getAlerts(userId?: string, role?: string): Observable<Alert[]> {
    let params: any = {};
    if (userId) params.userId = userId;
    if (role) params.role = role;
    return this.http.get<Alert[]>(this.apiUrl, { params });
  }

  getUnreadAlerts(userId?: string, role?: string): Observable<Alert[]> {
    let params: any = {};
    if (userId) params.userId = userId;
    if (role) params.role = role;
    return this.http.get<Alert[]>(`${this.apiUrl}/unread`, { params });
  }

  markAsRead(id: string): Observable<Alert> {
    return this.http.put<Alert>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(userId?: string, role?: string): Observable<void> {
    let params: any = {};
    if (userId) params.userId = userId;
    if (role) params.role = role;
    return this.http.put<void>(`${this.apiUrl}/read-all`, {}, { params });
  }

  deleteAlert(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  deleteAllAlerts(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/all`);
  }

  generateTestAlerts(): Observable<any> {
    return this.http.post(`${this.apiUrl}/generate-test`, {});
  }

  createAlert(title: string, message: string, type: string, category: string, relatedId?: string): Observable<void> {
    return this.http.post<void>(this.apiUrl, { title, message, type, category, relatedId });
  }

}
