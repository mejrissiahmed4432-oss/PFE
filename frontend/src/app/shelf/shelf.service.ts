import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Shelf } from './shelf.model';

@Injectable({
  providedIn: 'root'
})
export class ShelfService {
  private apiUrl = 'http://localhost:8000/api/shelves';

  constructor(private http: HttpClient) {}

  getAllShelves(): Observable<Shelf[]> {
    return this.http.get<Shelf[]>(this.apiUrl);
  }

  getShelfById(id: string): Observable<Shelf> {
    return this.http.get<Shelf>(`${this.apiUrl}/${id}`);
  }

  getShelvesByType(type: string): Observable<Shelf[]> {
    return this.http.get<Shelf[]>(`${this.apiUrl}/type/${type}`);
  }

  createShelf(shelf: Shelf): Observable<Shelf> {
    return this.http.post<Shelf>(this.apiUrl, shelf);
  }

  updateShelf(id: string, shelf: Shelf): Observable<Shelf> {
    return this.http.put<Shelf>(`${this.apiUrl}/${id}`, shelf);
  }

  deleteShelf(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
