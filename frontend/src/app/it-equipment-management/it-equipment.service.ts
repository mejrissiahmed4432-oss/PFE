import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8000/api/equipment';
const USERS_API = 'http://localhost:8000/api/users';
const EMPLOYEES_API = 'http://localhost:8000/api/employees';
const SHELVES_API = 'http://localhost:8000/api/shelves';
const DEPARTMENTS_API = 'http://localhost:8000/api/departments';

export interface ItEquipment {
  id: string;
  equipmentName: string;
  brand: string;
  supplier: string;
  model: string;
  type: string;
  category: string;
  serialNumber: string;
  shelfId: string;
  purchasePrice: number;
  purchaseDate: string;
  warrantyExpiration: string;
  invoiceRef: string;
  note: string;
  status: string;
  department: string;
  icon: string;
  qrCode: string;
  specifications: { [key: string]: string };
  itAssignedUserIds: string[];
  itAssignedUserNames: string[];
  itAssignedDepartmentId: string;
  itAssignedDepartmentName: string;
  itAssignedAt: string;
  returnRequested: boolean;
  returnNote: string;
  returnRequestedAt: string;
  lifecycle: LifecycleEntry[];
  qte: number;
}

export interface LifecycleEntry {
  status: string;
  timestamp: string;
  description: string;
  actor: string;
}

export interface AssignRequest {
  userIds?: string[];
  userNames?: string[];
  departmentId?: string;
  departmentName?: string;
  targetDepartment?: string;
  actor?: string;
}

@Injectable({ providedIn: 'root' })
export class ItEquipmentService {
  constructor(private http: HttpClient) {}

  getAvailableInStock(): Observable<ItEquipment[]> {
    return this.http.get<ItEquipment[]>(`${BASE}/it-available`);
  }

  getAllEquipment(): Observable<ItEquipment[]> {
    return this.http.get<ItEquipment[]>(BASE);
  }

  getAllInUse(): Observable<ItEquipment[]> {
    return this.http.get<ItEquipment[]>(`${BASE}/it-in-use`);
  }

  getAssignmentHistory(): Observable<ItEquipment[]> {
    return this.http.get<ItEquipment[]>(`${BASE}/it-assignment-history`);
  }

  getReturnRequests(): Observable<ItEquipment[]> {
    return this.http.get<ItEquipment[]>(`${BASE}/return-requests`);
  }

  getEquipmentById(id: string): Observable<ItEquipment> {
    return this.http.get<ItEquipment>(`${BASE}/${id}`);
  }

  assignEquipment(id: string, request: any): Observable<ItEquipment> {
    return this.http.post<ItEquipment>(`${BASE}/${id}/it-assign`, request);
  }

  deassignEquipment(id: string, actor: string): Observable<ItEquipment> {
    return this.http.post<ItEquipment>(`${BASE}/${id}/it-deassign`, { actor });
  }

  requestReturn(id: string, note: string, actor: string): Observable<ItEquipment> {
    return this.http.post<ItEquipment>(`${BASE}/${id}/request-return`, { note, actor });
  }

  processReturn(id: string, status: string, shelfId: string | null, actor: string): Observable<ItEquipment> {
    return this.http.post<ItEquipment>(`${BASE}/${id}/process-return`, { status, shelfId, actor });
  }

  getAllUsers(): Observable<any[]> {
    return this.http.get<any[]>(USERS_API);
  }

  getAllEmployees(): Observable<any[]> {
    return this.http.get<any[]>(EMPLOYEES_API);
  }

  getAllDepartments(): Observable<any[]> {
    return this.http.get<any[]>(DEPARTMENTS_API);
  }

  getAllShelves(): Observable<any[]> {
    return this.http.get<any[]>(SHELVES_API);
  }
}
