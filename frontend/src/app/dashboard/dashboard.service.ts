import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Equipment {
  id?: string;
  equipmentName: string;
  brand: string;
  category: string;
  location: string;
  createdAt?: string;
}

export interface DashboardStats {
  totalEquipment: number;
  totalSuppliers: number;
  warrantyExpiringSoon: number;
  lowStockAlerts: number;
  equipmentByCategory: { [key: string]: number };
  equipmentByLocation: { [key: string]: number };
  recentEquipment: Equipment[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = '/api/dashboard/stats';

  constructor(private http: HttpClient) { }

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(this.apiUrl);
  }
}
