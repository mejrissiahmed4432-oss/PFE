import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { TaskService } from './task.service';
import { AlertService } from '../alerts/alert.service';
import { Task } from './task.model';

@Component({
  selector: 'app-schedule',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './schedule.component.html',
  styleUrl: './schedule.component.css'
})
export class ScheduleComponent implements OnInit {
  currentUser: any;
  currentFilter: 'All' | 'Today' | 'Upcoming' | 'Completed' = 'All';
  selectedTask: Task | null = null;
  showAddModal = false;
  showDetailPanel = false;
  isSubmitting = false;

  // Toast notification
  toast: { message: string; type: 'success' | 'info' | 'error' } | null = null;

  // Calendar
  today = new Date();
  currentMonth = new Date();
  calendarDays: (Date | null)[] = [];
  weekDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  newTask: Partial<Task> = {
    title: '',
    description: '',
    type: 'General',
    priority: 'Medium',
    status: 'Pending',
    dueDate: '',
    assignedTo: ''
  };

  tasks: Task[] = [];

  constructor(
    private authService: AuthService,
    private taskService: TaskService,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.newTask = this.emptyTask();
    this.buildCalendar();
    this.loadTasks();
  }

  emptyTask(): Partial<Task> {
    const user = this.authService.getCurrentUser();
    return {
      title: '',
      description: '',
      type: 'General',
      priority: 'Medium',
      status: 'Pending',
      dueDate: '',
      assignedTo: user ? `${user.firstName} ${user.lastName || ''}`.trim() : ''
    };
  }

  showToast(message: string, type: 'success' | 'info' | 'error' = 'success') {
    this.toast = { message, type };
    setTimeout(() => this.toast = null, 3500);
  }

  loadTasks(): void {
    this.taskService.getTasks().subscribe({
      next: (data) => { this.tasks = data; },
      error: (err) => console.error('Failed to load tasks', err)
    });
  }

  buildCalendar(): void {
    const year = this.currentMonth.getFullYear();
    const month = this.currentMonth.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    this.calendarDays = [];
    for (let i = 0; i < firstDay; i++) this.calendarDays.push(null);
    for (let d = 1; d <= daysInMonth; d++) this.calendarDays.push(new Date(year, month, d));
  }

  prevMonth(): void {
    this.currentMonth = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() - 1, 1);
    this.buildCalendar();
  }

  nextMonth(): void {
    this.currentMonth = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() + 1, 1);
    this.buildCalendar();
  }

  getMonthLabel(): string {
    return this.currentMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  isToday(date: Date | null): boolean {
    if (!date) return false;
    return date.toDateString() === this.today.toDateString();
  }

  hasTasksOnDay(date: Date | null): Task[] {
    if (!date) return [];
    const s = date.toISOString().split('T')[0];
    return this.tasks.filter(t => t.dueDate === s);
  }

  get filteredTasks(): Task[] {
    if (!this.tasks) return [];
    const todayStr = this.today.toISOString().split('T')[0];
    switch (this.currentFilter) {
      case 'Today':     return this.tasks.filter(t => t.dueDate === todayStr);
      case 'Upcoming':  return this.tasks.filter(t => t.dueDate > todayStr && t.status !== 'Completed');
      case 'Completed': return this.tasks.filter(t => t.status === 'Completed');
      default:          return this.tasks;
    }
  }

  get stats() {
    if (!this.tasks) return { total: 0, completed: 0, inProgress: 0, pending: 0 };
    return {
      total:      this.tasks.length,
      completed:  this.tasks.filter(t => t.status === 'Completed').length,
      inProgress: this.tasks.filter(t => t.status === 'In Progress').length,
      pending:    this.tasks.filter(t => t.status === 'Pending').length,
    };
  }

  openDetail(task: Task): void {
    // Deep clone so changes don't reflect immediately in list
    this.selectedTask = { ...task };
    this.showDetailPanel = true;
  }

  closeDetail(): void {
    this.showDetailPanel = false;
    this.selectedTask = null;
  }

  updateTask(): void {
    if (!this.selectedTask || this.isSubmitting) return;
    this.isSubmitting = true;
    this.taskService.updateTask(this.selectedTask.id, this.selectedTask).subscribe({
      next: (updatedTask) => {
        const index = this.tasks.findIndex(t => t.id === updatedTask.id);
        if (index !== -1) this.tasks[index] = updatedTask;
        this.closeDetail();
        this.showToast(`✏️ Task "${updatedTask.title}" updated successfully`, 'info');
        this.alertService.createAlert(
          `Task Updated: ${updatedTask.title}`,
          `Task "${updatedTask.title}" was updated — Status: ${updatedTask.status}, Due: ${updatedTask.dueDate}`,
          'INFO', 'TASK', updatedTask.id
        ).subscribe();
        this.isSubmitting = false;
      },
      error: (err) => { console.error('Failed to update task', err); this.isSubmitting = false; }
    });
  }

  updateInlineStatus(task: Task): void {
    this.taskService.updateTaskStatus(task.id, task.status).subscribe({
      next: (updatedTask) => {
        const index = this.tasks.findIndex(t => t.id === task.id);
        if (index !== -1) this.tasks[index] = updatedTask;
        this.showToast(`Status changed to "${updatedTask.status}"`, 'success');
        this.alertService.createAlert(
          `Task Status Changed: ${updatedTask.title}`,
          `Status of "${updatedTask.title}" was changed to "${updatedTask.status}"`,
          updatedTask.status === 'Completed' ? 'SUCCESS' : 'INFO', 'TASK', updatedTask.id
        ).subscribe();
      },
      error: (err) => console.error('Failed to update inline task status', err)
    });
  }

  addTask(): void {
    if (!this.newTask.title || !this.newTask.dueDate || this.isSubmitting) return;
    this.isSubmitting = true;
    this.taskService.createTask(this.newTask).subscribe({
      next: (createdTask) => {
        this.tasks.unshift(createdTask);
        this.showAddModal = false;
        this.newTask = this.emptyTask();
        this.showToast(`✅ Task "${createdTask.title}" created!`, 'success');
        this.alertService.createAlert(
          `New Task Scheduled: ${createdTask.title}`,
          `A new task "${createdTask.title}" was created — Type: ${createdTask.type}, Priority: ${createdTask.priority}, Due: ${createdTask.dueDate}`,
          'INFO', 'TASK', createdTask.id
        ).subscribe();
        this.isSubmitting = false;
      },
      error: (err) => { console.error('Failed to create task', err); this.isSubmitting = false; }
    });
  }

  deleteTask(id: string): void {
    const task = this.tasks.find(t => t.id === id);
    this.taskService.deleteTask(id).subscribe({
      next: () => {
        this.tasks = this.tasks.filter(t => t.id !== id);
        this.closeDetail();
        this.showToast(`🗑️ Task deleted`, 'error');
      },
      error: (err) => console.error('Failed to delete task', err)
    });
  }

  getTypeColor(type: string): string {
    switch (type) {
      case 'Equipment':   return 'type-equipment';
      case 'Maintenance': return 'type-maintenance';
      case 'Stock':       return 'type-stock';
      default:            return 'type-general';
    }
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'High':   return 'priority-high';
      case 'Medium': return 'priority-medium';
      default:       return 'priority-low';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Completed':   return 'status-completed';
      case 'In Progress': return 'status-progress';
      default:            return 'status-pending';
    }
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch { return dateStr; }
  }
}
