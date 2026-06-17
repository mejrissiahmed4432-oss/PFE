import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaskService, TaskItem, TaskAssignRequest } from './task.service';
import { SystemUser } from '../user-management/user.service';
import { ItManagerService } from '../user-management/it-manager.service';

type WizardStep = 'task-info' | 'select-users';

@Component({
  selector: 'app-task-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './task-management.component.html',
  styleUrl: './task-management.component.css'
})
export class TaskManagementComponent implements OnInit {
  @Input() currentUserId: string = '';
  @Input() currentUserRole: string = '';

  // ── Data ────────────────────────────────────────────────────────────────────
  tasks: TaskItem[] = [];
  filteredTasks: TaskItem[] = [];
  systemUsers: SystemUser[] = [];

  // ── Filters ─────────────────────────────────────────────────────────────────
  statusFilter: string = 'all';
  priorityFilter: string = 'all';
  searchQuery: string = '';

  // ── Wizard Modal ────────────────────────────────────────────────────────────
  showWizard: boolean = false;
  wizardStep: WizardStep = 'task-info';
  isEditing: boolean = false;
  taskToEditId: string | null = null;

  customAlert: {
    type?: 'error' | 'warning',
    title: string,
    message: string,
    onConfirm?: () => void,
    onCancel?: () => void
  } | null = null;

  taskForm = {
    title: '',
    description: '',
    priority: 'Low',
    type: 'General',
    status: 'Pending',
    dueDate: ''
  };

// View Details Modal
showViewModal: boolean = false;
selectedTaskForView: TaskItem | null = null;

// Step 2: User Selection
userSearchQuery: string = '';
selectedUserIds: Set<string> = new Set();
inactiveUserWarnings: Set<string> = new Set();   // user IDs where user confirmed to include despite being inactive
pendingInactiveUserId: string | null = null;      // user ID waiting for A2 confirmation

// ── Delete Confirm ───────────────────────────────────────────────────────────
showDeleteModal: boolean = false;
taskToDelete: TaskItem | null = null;

// ── Status Change ────────────────────────────────────────────────────────────
showStatusModal: boolean = false;
taskToUpdateStatus: TaskItem | null = null;
newStatus: string = '';

// ── Toast ───────────────────────────────────────────────────────────────────
toast: { message: string; type: 'success' | 'error' } | null = null;

// ── Loading ─────────────────────────────────────────────────────────────────
isSubmitting: boolean = false;

  readonly PRIORITIES = [
  { value: 'Low', label: 'Low', color: '#22c55e' },
  { value: 'Medium', label: 'Medium', color: '#f59e0b' },
  { value: 'High', label: 'High', color: '#ef4444' }
];

  readonly TASK_TYPES = [
  'Equipment',
  'Maintenance',
  'Stock',
  'General'
];

  readonly STATUSES = ['Pending', 'In Progress', 'Completed'];

constructor(
  private taskService: TaskService,
  private itManagerService: ItManagerService
) { }

ngOnInit(): void {
  this.loadTasks();
  this.loadUsers();
}

// ── Load Data ────────────────────────────────────────────────────────────────
loadTasks(): void {
  const loader = this.currentUserId
    ? this.taskService.getTasksAssignedByManager(this.currentUserId)
    : this.taskService.getAllTasks();

  loader.subscribe({
    next: (tasks) => {
      this.tasks = tasks.sort((a, b) =>
        new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
      );
      this.applyFilters();
    },
    error: () => this.showToast('Failed to load tasks', 'error')
  });
}

loadUsers(): void {
  this.itManagerService.getUsers().subscribe({
    next: (users) => { this.systemUsers = users; },
    error: () => this.showToast('Failed to load users', 'error')
  });
}

// ── Filters ─────────────────────────────────────────────────────────────────
applyFilters(): void {
  const q = this.searchQuery.toLowerCase().trim();
  this.filteredTasks = this.tasks.filter(t => {
    const matchSearch = !q || t.title.toLowerCase().includes(q) || t.description?.toLowerCase().includes(q);
    const matchStatus = this.statusFilter === 'all' || t.status === this.statusFilter;
    const matchPriority = this.priorityFilter === 'all' || t.priority === this.priorityFilter;
    return matchSearch && matchStatus && matchPriority;
  });
}

openViewModal(task: TaskItem): void {
  this.selectedTaskForView = task;
  this.showViewModal = true;
}

closeViewModal(): void {
  this.showViewModal = false;
  this.selectedTaskForView = null;
}

// ── Wizard: Open / Close ─────────────────────────────────────────────────────
openWizard(): void {
  this.resetWizard();
  this.isEditing = false;
  this.taskToEditId = null;
  this.showWizard = true;
}

editTask(task: TaskItem | null | undefined): void {
  if(!task) return;
  this.closeViewModal();
  this.resetWizard();
  this.isEditing = true;
  this.taskToEditId = task.id;
  this.taskForm = {
    title: task.title,
    description: task.description || '',
    priority: task.priority,
    type: task.type,
    status: task.status || 'Pending',
    dueDate: task.dueDate
  };
  this.selectedUserIds = new Set(task.assignedUserIds || []);
  this.showWizard = true;
}

closeWizard(): void {
  this.showWizard = false;
  this.resetWizard();
  this.customAlert = null;
}

closeAlert(event ?: Event): void {
  if(event) event.stopPropagation();
  this.customAlert = null;
}

resetWizard(): void {
  this.wizardStep = 'task-info';
  this.taskForm = { title: '', description: '', priority: 'Low', type: 'General', status: 'Pending', dueDate: '' };
  this.selectedUserIds = new Set();
  this.userSearchQuery = '';
  this.isSubmitting = false;
}

// ── Wizard Step 1: Validation ─────────────────────────────────────────────
isStep1Valid(): boolean {
  return !!(
    this.taskForm.title.trim() &&
    this.taskForm.dueDate
  );
}

validateDueDate(event?: any): void {
  if (this.taskForm.dueDate) {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    const localTodayStr = `${yyyy}-${mm}-${dd}`;

    if (this.taskForm.dueDate < localTodayStr) {
      this.customAlert = {
        type: 'error',
        title: 'Validation Error',
        message: 'Target date cannot be in the past.'
      };
      
      this.taskForm.dueDate = ''; // reset model
      if (event && event.target) {
        event.target.value = ''; // force clear native input
      }
      
      // Fallback timeout just in case
      setTimeout(() => {
        this.taskForm.dueDate = '';
      });
    }
  }
}

goToStep2(): void {
  if(this.isStep1Valid()) {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    const localTodayStr = `${yyyy}-${mm}-${dd}`;

    if (this.taskForm.dueDate < localTodayStr) {
      this.customAlert = {
        type: 'error',
        title: 'Validation Error',
        message: 'Target date cannot be in the past.'
      };
      return;
    }
    this.wizardStep = 'select-users';
  }
}

  // ── Wizard Step 2: User Selection ─────────────────────────────────────────
  get filteredUsers(): SystemUser[] {
  const q = this.userSearchQuery.toLowerCase().trim();
  return this.systemUsers.filter(u =>
    !q ||
    u.firstName.toLowerCase().includes(q) ||
    u.lastName.toLowerCase().includes(q) ||
    u.email.toLowerCase().includes(q) ||
    u.role.toLowerCase().includes(q)
  );
}

toggleUserSelection(user: SystemUser): void {
  if(this.selectedUserIds.has(user.id)) {
  this.selectedUserIds.delete(user.id);
  this.selectedUserIds = new Set(this.selectedUserIds);
  return;
}

if (user.status !== 'ACTIVE') {
  this.customAlert = {
    type: 'warning',
    title: 'Selected user is not active.',
    message: 'Do you want to select them anyway?',
    onConfirm: () => {
      this.selectedUserIds.add(user.id);
      this.selectedUserIds = new Set(this.selectedUserIds);
      this.customAlert = null;
    },
    onCancel: () => {
      this.customAlert = null;
    }
  };
  return;
}

this.selectedUserIds.add(user.id);
this.selectedUserIds = new Set(this.selectedUserIds);
  }

isUserSelected(userId: string): boolean {
  return this.selectedUserIds.has(userId);
}

// ── Assign Task ────────────────────────────────────────────────────────────
assignTask(): void {
  if(this.selectedUserIds.size === 0) {
  this.showToast('Please select at least one user', 'error');
  return;
}
this.isSubmitting = true;

if (this.isEditing && this.taskToEditId) {
  const taskPayload: any = {
    id: this.taskToEditId,
    title: this.taskForm.title,
    description: this.taskForm.description,
    priority: this.taskForm.priority,
    type: this.taskForm.type,
    status: this.taskForm.status,
    dueDate: this.taskForm.dueDate,
    assignedUserIds: Array.from(this.selectedUserIds)
  };
  this.taskService.updateTask(this.taskToEditId, taskPayload).subscribe({
    next: (task) => {
      this.isSubmitting = false;
      this.showToast(`Task "${task.title}" updated successfully!`, 'success');
      this.closeWizard();
      this.loadTasks();
    },
    error: (err) => {
      this.isSubmitting = false;
      this.showToast(err.error?.error || 'Failed to update task', 'error');
    }
  });
} else {
  const request: TaskAssignRequest = {
    title: this.taskForm.title,
    description: this.taskForm.description,
    priority: this.taskForm.priority,
    type: this.taskForm.type,
    dueDate: this.taskForm.dueDate,
    assignedByUserId: this.currentUserId,
    assignedUserIds: Array.from(this.selectedUserIds)
  };

  this.taskService.assignTask(request).subscribe({
    next: (task) => {
      this.isSubmitting = false;
      this.showToast(`Task "${task.title}" assigned to ${this.selectedUserIds.size} user(s) successfully!`, 'success');
      this.closeWizard();
      this.loadTasks();
    },
    error: (err) => {
      this.isSubmitting = false;
      this.showToast(err.error?.message || err.error?.error || 'Failed to assign task', 'error');
    }
  });
}
  }

// ── Status Update ──────────────────────────────────────────────────────────
openStatusModal(task: TaskItem): void {
  this.taskToUpdateStatus = task;
  this.newStatus = task.status;
  this.showStatusModal = true;
}

closeStatusModal(): void {
  this.showStatusModal = false;
  this.taskToUpdateStatus = null;
}

saveStatus(): void {
  if(!this.taskToUpdateStatus) return;
  this.taskService.updateTaskStatus(this.taskToUpdateStatus.id, this.newStatus).subscribe({
    next: () => {
      this.showToast('Task status updated', 'success');
      this.closeStatusModal();
      this.loadTasks();
    },
    error: (err) => this.showToast(err.error?.error || 'Failed to update status', 'error')
  });
}

// ── Delete ─────────────────────────────────────────────────────────────────
openDeleteModal(task: TaskItem): void {
  this.taskToDelete = task;
  this.showDeleteModal = true;
}

closeDeleteModal(): void {
  this.showDeleteModal = false;
  this.taskToDelete = null;
}

confirmDelete(): void {
  if(!this.taskToDelete) return;
  this.taskService.deleteTask(this.taskToDelete.id).subscribe({
    next: () => {
      this.showToast('Task deleted successfully', 'success');
      this.closeDeleteModal();
      this.loadTasks();
    },
    error: (err) => this.showToast(err.error?.error || 'Failed to delete task', 'error')
  });
}

// ── Helpers ────────────────────────────────────────────────────────────────
  getAssigneeNames(task: TaskItem | null | undefined): string {
    if (!task || !task.assignedUserIds?.length) return '—';
    return task.assignedUserIds
      .map(uid => {
        const u = this.systemUsers.find(u => u.id === uid);
        return u ? `${u.firstName} ${u.lastName}` : uid;
      })
      .join(', ');
  }

  getAssignedByName(task: TaskItem | null | undefined): string {
    if (!task || !task.assignedByUserId) return '—';
    if (task.assignedByUserId === this.currentUserId) return 'me';
    const u = this.systemUsers.find(u => u.id === task.assignedByUserId);
    return u ? `${u.firstName} ${u.lastName}` : task.assignedByUserId;
  }

getPriorityColor(priority: string): string {
  return this.PRIORITIES.find(p => p.value === priority)?.color || '#94a3b8';
}

  getStatusClass(status: string): string {
    switch (status) {
      case 'Pending': return 'status-todo';
      case 'In Progress': return 'status-progress';
      case 'Completed': return 'status-done';
      case 'History': return 'status-history';
      default: return 'status-todo';
    }
  }

formatDate(dateStr: string): string {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) + 
         ' ' + 
         date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
}

isTaskPastDue(task: TaskItem | null | undefined): boolean {
  if (!task || !task.dueDate) return false;
  const todayStr = new Date().toISOString().split('T')[0];
  return task.dueDate < todayStr;
}

getTaskStats() {
  return {
    total: this.tasks.length,
    todo: this.tasks.filter(t => t.status === 'Pending').length,
    inProgress: this.tasks.filter(t => t.status === 'In Progress').length,
    done: this.tasks.filter(t => t.status === 'Completed').length,
  };
}

getUserStatusClass(status: string): string {
  switch (status) {
    case 'ACTIVE': return 'user-active';
    case 'INACTIVE': return 'user-inactive';
    default: return 'user-pending';
  }
}

showToast(message: string, type: 'success' | 'error'): void {
  this.toast = { message, type };
  setTimeout(() => { this.toast = null; }, 4500);
  }
}
