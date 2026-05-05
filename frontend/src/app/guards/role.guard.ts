import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  const user = authService.getCurrentUser();
  if (user) {
    // If the route has expected roles defined
    const expectedRoles = route.data['roles'] as string[];
    
    // Admin has access to everything
    if (user.role === 'ADMIN') {
      return true;
    }
    
    if (expectedRoles && expectedRoles.length > 0) {
      if (expectedRoles.includes(user.role)) {
        return true;
      } else {
        // Redirect to a default route or show unauthorized
        router.navigate(['/login']);
        return false;
      }
    }
    
    // If no specific roles are required, but user is logged in
    return true;
  }

  router.navigate(['/login']);
  return false;
};
