import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Department } from './department.model';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private apiUrl = '/api/departments';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Department[]> {
    return this.http.get<Department[]>(this.apiUrl);
  }

  getById(id: string): Observable<Department> {
    return this.http.get<Department>(`${this.apiUrl}/${id}`);
  }

  create(dept: Department): Observable<Department> {
    return this.http.post<Department>(this.apiUrl, dept);
  }

  update(id: string, dept: Department): Observable<Department> {
    return this.http.put<Department>(`${this.apiUrl}/${id}`, dept);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
