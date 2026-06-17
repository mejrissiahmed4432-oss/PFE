import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ItManagerService } from './it-manager.service';
import { SystemUser, ProvisionRequest } from './user.service';
import { Employee } from '../employee/employee.model';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css'
})
export class UserManagementComponent implements OnInit, OnDestroy {

  // ── Data ──────────────────────────────────────
  employees: Employee[] = [];
  systemUsers: SystemUser[] = [];
  filteredEmployees: Employee[] = [];

  // ── Search & Filter ───────────────────────────
  employeeSearch: string = '';
  accessFilter: 'all' | 'hasAccess' | 'noAccess' = 'all';

  userSearch: string = '';
  roleFilter: string = 'all';
  statusFilter: string = 'all';
  filteredSystemUsers: SystemUser[] = [];

  // ── Tabs ──────────────────────────────────────
  activeTab: 'grant' | 'manage' = 'grant';

  // ── Modal: Grant Access ────────────────────────
  showGrantModal: boolean = false;
  selectedEmployee: Employee | null = null;
  selectedRole: string = 'TECHNICIAN';
  isSubmitting: boolean = false;

  // ── Modal: Change Role ────────────────────────
  showRoleModal: boolean = false;
  selectedUser: SystemUser | null = null;
  newRole: string = '';

  // ── Modal: Revoke Confirm ─────────────────────
  showRevokeModal: boolean = false;
  userToRevoke: SystemUser | null = null;

  // ── Modal: Details ────────────────────────────
  showDetailsModal: boolean = false;
  selectedUserDetails: SystemUser | null = null;

  // ── Feedback ──────────────────────────────────
  toast: { message: string; type: 'success' | 'error' } | null = null;

  // ── WebSocket ─────────────────────────────────
  private stompClient: Client | null = null;

  readonly ROLES = [
    { value: 'TECHNICIAN', label: 'Technician', icon: 'wrench' },
    { value: 'STOCK_MANAGER', label: 'Stock Manager', icon: 'box' },
    { value: 'USER', label: 'User', icon: 'users' },
    { value: 'IT_MANAGER', label: 'IT Manager', icon: 'laptop' },
    { value: 'ADMIN', label: 'Admin', icon: 'settings' },
  ];

  getAvailableRoles(currentUserId?: string) {
    const assignedRoles = new Set(
      this.systemUsers
        .filter(u => u.status !== 'INACTIVE' && u.id !== currentUserId)
        .map(u => u.role)
    );
    return this.ROLES.filter(r => {
      if (['ADMIN', 'IT_MANAGER', 'STOCK_MANAGER'].includes(r.value)) {
        return !assignedRoles.has(r.value);
      }
      return true;
    });
  }

  constructor(
    private itManagerService: ItManagerService
  ) {}

  ngOnInit(): void {
    this.load();
    this.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.disconnectWebSocket();
  }

  // ── WebSocket ─────────────────────────────────
  connectWebSocket(): void {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8000/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        // Subscribe to user status changes broadcast by the backend
        this.stompClient!.subscribe('/topic/user-status', (message) => {
          const event: { userId: string; status?: string; email?: string; lastLogin?: string; online?: boolean } = JSON.parse(message.body);
          console.log('[WebSocket] User status update received:', event);

          // Update the affected user in-place — no HTTP call needed
          const idx = this.systemUsers.findIndex(u => u.id === event.userId || (event.email && u.email === event.email));
          if (idx !== -1) {
            this.systemUsers[idx] = { 
                ...this.systemUsers[idx], 
                status: event.status ? (event.status as 'PENDING' | 'ACTIVE' | 'INACTIVE') : this.systemUsers[idx].status,
                lastLogin: event.lastLogin ? event.lastLogin : this.systemUsers[idx].lastLogin,
                online: event.online !== undefined ? event.online : this.systemUsers[idx].online
            };
            this.systemUsers = [...this.systemUsers]; // trigger Angular change detection
          }

          // Also refresh employees to sync hasAccess()
          this.itManagerService.getEmployees().subscribe({
            next: (emps) => { 
              this.employees = emps; 
              this.filterEmployees(); 
              this.filterUsers(); 
            }
          });
        });
      },
      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame);
      }
    });

    this.stompClient.activate();
  }

  disconnectWebSocket(): void {
    if (this.stompClient?.active) {
      this.stompClient.deactivate();
    }
  }

  load(): void {
    this.itManagerService.getEmployees().subscribe({
      next: (emps) => {
        this.employees = emps;
        this.filterEmployees();
      },
      error: () => this.showToast('Failed to load employees', 'error')
    });
    this.itManagerService.getUsers().subscribe({
      next: (users) => { 
        this.systemUsers = users; 
        this.filterUsers();
      },
      error: () => this.showToast('Failed to load users', 'error')
    });
  }

  // ── Employees ─────────────────────────────────
  filterEmployees(): void {
    const q = this.employeeSearch.toLowerCase().trim();
    this.filteredEmployees = this.employees.filter(e => {
      const matchesSearch = !q ||
        e.firstName.toLowerCase().includes(q) ||
        e.lastName.toLowerCase().includes(q) ||
        e.email.toLowerCase().includes(q) ||
        e.department.toLowerCase().includes(q) ||
        e.jobTitle.toLowerCase().includes(q) ||
        (e.cin && e.cin.toLowerCase().includes(q));

      const hasAcc = this.hasAccess(e);
      const matchesAccess = this.accessFilter === 'all' || 
                           (this.accessFilter === 'hasAccess' && hasAcc) ||
                           (this.accessFilter === 'noAccess' && !hasAcc);

      return matchesSearch && matchesAccess;
    });
  }

  filterUsers(): void {
    const q = this.userSearch.toLowerCase().trim();
    this.filteredSystemUsers = this.systemUsers.filter(u => {
      const matchesSearch = !q ||
        u.firstName.toLowerCase().includes(q) ||
        u.lastName.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        u.id.toLowerCase().includes(q) ||
        (u.phoneNumber && u.phoneNumber.toLowerCase().includes(q)) ||
        (u.role && u.role.toLowerCase().includes(q));

      const matchesRole = this.roleFilter === 'all' || u.role === this.roleFilter;
      const matchesStatus = this.statusFilter === 'all' || u.status === this.statusFilter;

      return matchesSearch && matchesRole && matchesStatus;
    });
  }

  hasAccess(emp: Employee): boolean {
    return !!emp.userId || this.systemUsers.some(u => u.email === emp.email);
  }

  getUserForEmployee(emp: Employee): SystemUser | undefined {
    return this.systemUsers.find(u => u.email === emp.email || u.id === emp.userId);
  }

  // ── Grant Modal ───────────────────────────────
  openGrantModal(emp: Employee): void {
    this.selectedEmployee = emp;
    const available = this.getAvailableRoles();
    this.selectedRole = available.length > 0 ? available[0].value : 'TECHNICIAN';
    this.showGrantModal = true;
  }

  closeGrantModal(): void {
    this.showGrantModal = false;
    this.selectedEmployee = null;
  }

  grantAccess(): void {
    if (!this.selectedEmployee) return;
    this.isSubmitting = true;
    const payload: ProvisionRequest = {
      email: this.selectedEmployee.email,
      firstName: this.selectedEmployee.firstName,
      lastName: this.selectedEmployee.lastName,
      role: this.selectedRole,
      employeeId: this.selectedEmployee.id
    };
    this.itManagerService.provisionUser(payload).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.showGrantModal = false;
        this.showToast(`Access granted! Welcome email sent to ${this.selectedEmployee!.email}`, 'success');
        this.load();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.showToast(err.error?.message || 'Failed to grant access', 'error');
      }
    });
  }

  // ── Role Modal ────────────────────────────────
  openRoleModal(user: SystemUser): void {
    this.selectedUser = user;
    const available = this.getAvailableRoles(user.id);
    this.newRole = user.role;
    if (!available.find(r => r.value === this.newRole) && available.length > 0) {
      this.newRole = available[0].value;
    }
    this.showRoleModal = true;
  }

  closeRoleModal(): void {
    this.showRoleModal = false;
    this.selectedUser = null;
  }

  saveRole(): void {
    if (!this.selectedUser) return;
    this.itManagerService.updateUserRole(this.selectedUser.id, this.newRole).subscribe({
      next: () => {
        this.showToast('Role updated successfully', 'success');
        this.showRoleModal = false;
        this.load();
      },
      error: (err) => this.showToast(err.error?.message || 'Failed to update role', 'error')
    });
  }

  // ── Details Modal ──────────────────────────────
  openDetailsModal(user: SystemUser): void {
    this.selectedUserDetails = user;
    this.showDetailsModal = true;
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedUserDetails = null;
  }

  // ── Revoke ────────────────────────────────────
  openRevokeModal(user: SystemUser): void {
    this.userToRevoke = user;
    this.showRevokeModal = true;
  }

  closeRevokeModal(): void {
    this.showRevokeModal = false;
    this.userToRevoke = null;
  }

  revokeAccess(): void {
    if (!this.userToRevoke) return;
    this.itManagerService.deleteUser(this.userToRevoke.id).subscribe({
      next: () => {
        this.showToast(`Access revoked for ${this.userToRevoke!.firstName}`, 'success');
        this.showRevokeModal = false;
        this.load();
      },
      error: (err) => this.showToast(err.error?.message || 'Failed to revoke access', 'error')
    });
  }

  // ── Resend Invitation ─────────────────────────
  resendInvitation(user: SystemUser): void {
    this.itManagerService.resendInvitation(user.id).subscribe({
      next: () => this.showToast(`Invitation resent to ${user.email}`, 'success'),
      error: (err) => this.showToast(err.error?.message || 'Failed to resend', 'error')
    });
  }

  toggleStatus(user: SystemUser): void {
    const newStatus = user.status === 'INACTIVE' ? 'ACTIVE' : 'INACTIVE';
    this.itManagerService.updateUserStatus(user.id, newStatus).subscribe({
      next: () => {
        this.showToast(`User ${newStatus === 'ACTIVE' ? 'activated' : 'deactivated'}`, 'success');
        this.load();
      },
      error: (err) => this.showToast(err.error?.message || 'Failed to update status', 'error')
    });
  }

  // ── Helpers ───────────────────────────────────
  getRoleLabel(role: string): string {
    return this.ROLES.find(r => r.value === role)?.label || role;
  }

  getRoleIcon(role: string): string {
    return this.ROLES.find(r => r.value === role)?.icon || '👤';
  }

  showToast(message: string, type: 'success' | 'error'): void {
    this.toast = { message, type };
    setTimeout(() => { this.toast = null; }, 4000);
  }
}
