import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { BoardComponent } from './board/board.component';
import { roleGuard } from './guards/role.guard';
import { SupplierRespondComponent } from './supplier-respond/supplier-respond.component';

export const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: LoginComponent },
    { path: 'supplier-respond/:token', component: SupplierRespondComponent },
    { path: 'board', component: BoardComponent, canActivate: [roleGuard] },
];

