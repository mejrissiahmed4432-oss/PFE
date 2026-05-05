import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HrDashboardStats } from './hr-dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class HrDashboardService {

  constructor(private http: HttpClient) { }

  getDashboardStats(): Observable<HrDashboardStats> {
    return this.http.get<HrDashboardStats>('/api/hr/dashboard/stats');
  }
}
