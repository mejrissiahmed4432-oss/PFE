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
    const userData = sessionStorage.getItem('user_data');
    if (userData) {
      this.currentUser = JSON.parse(userData);
      // If we don't have an ID, we should try to sync from the server
      if (!this.currentUser.id && this.isLoggedIn()) {
        setTimeout(() => this.syncUserProfile(), 100);
      }
      return this.currentUser;
    }
    return null;
  }

  syncUserProfile() {
    this.http.get(`${this.apiUrl}/me`).subscribe({
      next: (response: any) => {
        const updated = { ...this.currentUser, ...response };
        this.currentUser = updated;
        sessionStorage.setItem('user_data', JSON.stringify(updated));
        this.userSubject.next(updated);
      },
      error: () => { } // Ignore on fail
    });
  }

  login(credentials: { email: string, password: any }): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: any) => {
        this.currentUser = response;
        if (response.token) {
          sessionStorage.setItem('auth_token', response.token);
          sessionStorage.setItem('user_data', JSON.stringify(response));
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
      const userData = sessionStorage.getItem('user_data');
      if (userData) {
        this.currentUser = JSON.parse(userData);
      }
    }
    if (this.currentUser && !this.currentUser.id && this.isLoggedIn()) {
       this.syncUserProfile();
       // Return what we have for now, it'll update shortly via userSubject
    }
    return this.currentUser;
  }

  logout() {
    this.currentUser = null;
    sessionStorage.removeItem('auth_token');
    sessionStorage.removeItem('user_data');
    this.userSubject.next(null);
  }

  isLoggedIn(): boolean {
    return sessionStorage.getItem('auth_token') !== null;
  }

  updateProfile(userData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/profile`, userData).pipe(
      tap((response: any) => {
        // Update local session data with new values
        const current = this.getCurrentUser();
        const updated = { ...current, ...response };
        this.currentUser = updated;
        sessionStorage.setItem('user_data', JSON.stringify(updated));
        this.userSubject.next(updated);
      })
    );
  }
}

