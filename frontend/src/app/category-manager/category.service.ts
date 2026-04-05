import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EquipmentCategory } from './category.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private apiUrl = '/api/equipment-categories';

  constructor(private http: HttpClient) {}

  getAllCategories(): Observable<EquipmentCategory[]> {
    return this.http.get<EquipmentCategory[]>(this.apiUrl);
  }

  getCategoryById(id: string): Observable<EquipmentCategory> {
    return this.http.get<EquipmentCategory>(`${this.apiUrl}/${id}`);
  }

  createCategory(category: EquipmentCategory): Observable<EquipmentCategory> {
    return this.http.post<EquipmentCategory>(this.apiUrl, category);
  }

  updateCategory(id: string, category: EquipmentCategory): Observable<EquipmentCategory> {
    return this.http.put<EquipmentCategory>(`${this.apiUrl}/${id}`, category);
  }

  addTypeToCategory(id: string, type: string): Observable<EquipmentCategory> {
    return this.http.post<EquipmentCategory>(`${this.apiUrl}/${id}/types`, type);
  }

  removeTypeFromCategory(id: string, type: string): Observable<EquipmentCategory> {
    return this.http.delete<EquipmentCategory>(`${this.apiUrl}/${id}/types/${type}`);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
