import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { HttpClient, HttpBackend } from '@angular/common/http';
import { catchError, switchMap, throwError } from 'rxjs';

const GATEWAY_URL = 'http://localhost:8000';

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  // Inject HttpBackend at the root of the function to satisfy Angular's injection context rules.
  // Using HttpBackend to create a new HttpClient ensures this request bypasses all interceptors,
  // preventing circular dependencies and infinite loops during token refresh.
  const httpBackend = inject(HttpBackend);
  const http = new HttpClient(httpBackend);

  const token = sessionStorage.getItem('auth_token');

  // Attach the access token to every outgoing request
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

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
        const refreshToken = sessionStorage.getItem('refresh_token');
        if (!refreshToken) {
          // No refresh token available → redirect to login
          sessionStorage.clear();
          window.location.href = '/login';
          return throwError(() => error);
        }

        return http.post<{ token: string }>(`${GATEWAY_URL}/api/users/refresh-token`, { refreshToken }).pipe(
          switchMap((response) => {
            // Save the new access token
            sessionStorage.setItem('auth_token', response.token);
            // Retry the original failed request with the new token
            const retryReq = req.clone({
              setHeaders: { Authorization: `Bearer ${response.token}` }
            });
            return next(retryReq);
          }),
          catchError((refreshError) => {
            // Refresh token is also expired or invalid → force re-login
            sessionStorage.clear();
            window.location.href = '/login';
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
