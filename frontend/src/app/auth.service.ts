import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/users';
  private currentUser: any = null;
  private userSubject = new BehaviorSubject<any>(this.getInitialUser());
  user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient) { }

  private getInitialUser() {
    const userData = localStorage.getItem('user_data');
    if (userData) {
      this.currentUser = JSON.parse(userData);
      return this.currentUser;
    }
    return null;
  }

  login(credentials: { email: string, password: any }): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: any) => {
        this.currentUser = response;
        if (response.token) {
          localStorage.setItem('auth_token', response.token);
          localStorage.setItem('user_data', JSON.stringify(response));
          this.userSubject.next(response);
        }
      })
    );
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(data: { token: string, newPassword: any }): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password`, data);
  }

  getCurrentUser() {
    if (!this.currentUser) {
      const userData = localStorage.getItem('user_data');
      if (userData) {
        this.currentUser = JSON.parse(userData);
      }
    }
    return this.currentUser;
  }

  logout() {
    this.currentUser = null;
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_data');
    this.userSubject.next(null);
  }

  isLoggedIn(): boolean {
    return localStorage.getItem('auth_token') !== null;
  }

  updateProfile(userData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/profile`, userData).pipe(
      tap((response: any) => {
        // Update local session data with new values
        const current = this.getCurrentUser();
        const updated = { ...current, ...response };
        this.currentUser = updated;
        localStorage.setItem('user_data', JSON.stringify(updated));
        this.userSubject.next(updated);
      })
    );
  }
}

