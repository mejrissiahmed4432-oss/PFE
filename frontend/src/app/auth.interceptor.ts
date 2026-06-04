import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { HttpClient, HttpBackend } from '@angular/common/http';
import { catchError, switchMap, filter, take, throwError, BehaviorSubject } from 'rxjs';
import { GlobalErrorService } from './core/services/global-error.service';

const GATEWAY_URL = 'http://localhost:8000';

let isRefreshing = false;
let refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  // Inject HttpBackend at the root of the function to satisfy Angular's injection context rules.
  // Using HttpBackend to create a new HttpClient ensures this request bypasses all interceptors,
  // preventing circular dependencies and infinite loops during token refresh.
  const httpBackend = inject(HttpBackend);
  const http = new HttpClient(httpBackend);
  const globalErrorService = inject(GlobalErrorService);

  const token = sessionStorage.getItem('auth_token');
  const userData = sessionStorage.getItem('user_data');
  let userCIN = '';
  let userName = '';
  let userRole = '';
  let userEmail = '';
  
  if (userData) {
    try {
      const parsed = JSON.parse(userData);
      userCIN = parsed.employeeId || parsed.id || '';
      userRole = parsed.role || '';
      userEmail = parsed.email || '';
      userName = (parsed.firstName || '') + ' ' + (parsed.lastName || '');
    } catch (_) {}
  }

  // Attach the access token to every outgoing request (except refresh-token)
  let authReq = req;
  if (token && !req.url.includes('/api/users/refresh-token')) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
        'X-User-CIN': userCIN,
        'X-User-Name': userName.trim(),
        'X-User-Role': userRole,
        'X-User-Email': userEmail
      }
    });
  }

  return next(authReq).pipe(
    catchError((error) => {
      // Only intercept 401 Unauthorized errors — but NOT for auth routes (avoid infinite loops)
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !req.url.includes('/api/users/refresh-token') &&
        !req.url.includes('/api/users/login') &&
        !req.url.includes('/api/public/')
      ) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null); // Reset the subject to null while refreshing

          const refreshToken = sessionStorage.getItem('refresh_token');
          if (!refreshToken) {
            // No refresh token available → redirect to login
            isRefreshing = false;
            sessionStorage.clear();
            window.location.href = '/login';
            return throwError(() => error);
          }

          return http.post<{ token: string, refreshToken?: string }>(`${GATEWAY_URL}/api/users/refresh-token`, { refreshToken }).pipe(
            switchMap((response) => {
              isRefreshing = false;
              // Save the new access token
              sessionStorage.setItem('auth_token', response.token);
              // Save the new refresh token if the backend provided one (Refresh Token Rotation)
              if (response.refreshToken) {
                sessionStorage.setItem('refresh_token', response.refreshToken);
              }
              // Notify all queued requests that the token is ready
              refreshTokenSubject.next(response.token);
              
              // Retry the original failed request with the new token
              const retryReq = req.clone({
                setHeaders: { Authorization: `Bearer ${response.token}` }
              });
              return next(retryReq);
            }),
            catchError((refreshError) => {
              // Refresh token is also expired or invalid → force re-login
              isRefreshing = false;
              sessionStorage.clear();
              window.location.href = '/login';
              return throwError(() => refreshError);
            })
          );
        } else {
          // A refresh is already in progress. Wait for it to complete.
          return refreshTokenSubject.pipe(
            filter(newToken => newToken !== null), // Wait until token is not null
            take(1), // Complete the observable after getting 1 token to prevent memory leaks
            switchMap(newToken => {
              // Retry the original failed request with the new token
              const retryReq = req.clone({
                setHeaders: { Authorization: `Bearer ${newToken}` }
              });
              return next(retryReq);
            })
          );
        }
      } else if (error instanceof HttpErrorResponse && (error.status === 503 || error.status === 500)) {
        // Trigger global error dialog with the EXACT requested message
        const errorMsg = error.error?.error || 'Service is not available. Please try again later or contact the Administrator';
        
        globalErrorService.showError(errorMsg);
      }
      return throwError(() => error);
    })
  );
};
