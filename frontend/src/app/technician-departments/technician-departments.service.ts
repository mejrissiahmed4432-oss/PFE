import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LaptopStatus {
  equipmentId: string;
  equipmentName: string;
  serialNumber: string;
  brand: string;
  model: string;
  department: string;
  ip: string | null;
  upStatus: 'UP' | 'DOWN' | 'NOT_FOUND_YET';
}

export interface DeptPcSummary {
  departmentName: string;
  totalLaptops: number;
  onlineCount: number;
  offlineCount: number;
  notFoundCount: number;
  laptops: LaptopStatus[];
}

@Injectable({ providedIn: 'root' })
export class TechnicianDeptService {
  private readonly API = 'http://localhost:8000/api/monitoring/dept-pc-status';

  constructor(private http: HttpClient) {}

  getDeptPcStatus(): Observable<DeptPcSummary[]> {
    return this.http.get<DeptPcSummary[]>(this.API);
  }
}
