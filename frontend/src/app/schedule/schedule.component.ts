import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { TaskService } from './task.service';
import { AlertService } from '../alerts/alert.service';
import { DragDropModule, CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Task } from './task.model';

@Component({
  selector: 'app-schedule',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
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
  
  customAlert: { title: string, message: string } | null = null;

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
<<<<<<< HEAD
      assignedTo: user ? `${user.firstName} ${user.lastName || ''}`.trim() : ''
=======
      assignedTo: user ? `${user.firstName} ${user.lastName || ''}`.trim() : '',
      userId: user ? user.id : ''
>>>>>>> my-local-work
    };
  }

  showToast(message: string, type: 'success' | 'info' | 'error' = 'success') {
    this.toast = { message, type };
    setTimeout(() => this.toast = null, 3500);
  }

  loadTasks(): void {
<<<<<<< HEAD
    this.taskService.getTasks().subscribe({
      next: (data) => { 
        data.forEach(t => this.processTask(t));

        if (this.currentUser) {
          const userFullName = `${this.currentUser.firstName} ${this.currentUser.lastName || ''}`.trim();
          this.tasks = data.filter(t => t.assignedTo === userFullName);
        } else {
          this.tasks = data; 
        }
=======
    if (!this.currentUser) return;

    this.taskService.getTasks(this.currentUser.id).subscribe({
      next: (data) => { 
        data.forEach(t => this.processTask(t));
        this.tasks = data;
>>>>>>> my-local-work
      },
      error: (err) => console.error('Failed to load tasks', err)
    });
  }

  processTask(t: Task): void {
    const todayStr = this.formatDateKey(this.today);
    
    // If the task already has an original due date, it means it was rolled over before
    if (t.originalDueDate) {
      (t as any).isOverdue = true;
    }

    if (t.dueDate < todayStr) {
      if (t.status === 'Completed') {
        t.status = 'History';
        this.taskService.updateTaskStatus(t.id, 'History').subscribe();
      } else if (t.status === 'Pending' || t.status === 'In Progress') {
        (t as any).isOverdue = true;
        if (!t.originalDueDate) {
          t.originalDueDate = t.dueDate;
        }
        t.dueDate = todayStr;
        this.taskService.updateTask(t.id, t).subscribe();
      }
    }
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
    const s = this.formatDateKey(date);
    return this.tasks.filter(t => t.dueDate === s);
  }

  selectedDayFilter: Date | null = this.today;

  toggleDayFilter(day: Date | null): void {
    if (!day) return;
    if (this.selectedDayFilter && this.isSameDay(this.selectedDayFilter, day)) {
      // Revert to today if user tries to unselect
      this.selectedDayFilter = this.today;
    } else {
      this.selectedDayFilter = day;
    }
  }

  hasHistoryTasksOnDay(date: Date | null): boolean {
    if (!date) return false;
    const s = this.formatDateKey(date);
    return this.tasks.some(t => t.dueDate === s && t.status === 'History');
  }

  formatDateKey(date: Date): string {
    const y = date.getFullYear();
    const m = (date.getMonth() + 1).toString().padStart(2, '0');
    const d = date.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  isSameDay(d1: Date, d2: Date): boolean {
    return d1.getFullYear() === d2.getFullYear() && 
           d1.getMonth() === d2.getMonth() && 
           d1.getDate() === d2.getDate();
  }

  get filteredTasks(): Task[] {
    if (!this.tasks) return [];
    let result = this.tasks;
    
    const todayStr = this.formatDateKey(this.today);
    switch (this.currentFilter) {
      case 'Today':     
        result = result.filter(t => t.dueDate === todayStr); 
        break;
      case 'Upcoming':  result = result.filter(t => t.dueDate > todayStr && t.status !== 'Completed' && t.status !== 'History'); break;
      case 'Completed': result = result.filter(t => t.status === 'Completed'); break;
    }
    
    if (this.selectedDayFilter) {
      const selStr = this.formatDateKey(this.selectedDayFilter);
      result = result.filter(t => t.dueDate === selStr);
    }

    const priorityWeights: Record<string, number> = { 'High': 0, 'Medium': 1, 'Low': 2 };
    return result.sort((a, b) => (priorityWeights[a.priority] ?? 3) - (priorityWeights[b.priority] ?? 3));
  }

  get todoTasks(): Task[] {
    return this.filteredTasks.filter(t => t.status === 'Pending');
  }

  get inProgressTasks(): Task[] {
    return this.filteredTasks.filter(t => t.status === 'In Progress');
  }

  get doneTasks(): Task[] {
    return this.filteredTasks.filter(t => t.status === 'Completed');
  }

  get historyTasks(): Task[] {
    return this.filteredTasks.filter(t => t.status === 'History');
  }

  get stats() {
    if (!this.tasks) return { total: 0, completed: 0, inProgress: 0, pending: 0 };
    return {
      total:      this.tasks.filter(t => t.status !== 'History').length,
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

  onDrop(event: CdkDragDrop<Task[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const task = event.previousContainer.data[event.previousIndex];
      const targetListId = event.container.id; 
      
      let newStatus: 'Pending' | 'In Progress' | 'Completed' | 'History' = 'Pending';
      if (targetListId === 'list-progress') newStatus = 'In Progress';
      else if (targetListId === 'list-done') newStatus = 'Completed';
      else if (targetListId === 'list-history') newStatus = 'History';

      task.status = newStatus;
      
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );

      this.taskService.updateTaskStatus(task.id, newStatus).subscribe({
        next: (updatedTask) => {
           this.processTask(updatedTask);
           this.alertService.createAlert(
              `Task Moved: ${updatedTask.title}`,
              `Task was moved to "${updatedTask.status}"`,
              updatedTask.status === 'Completed' ? 'SUCCESS' : 'INFO', 'TASK', updatedTask.id
           ).subscribe();
        },
        error: (err) => console.error('Failed to update task status automatically', err)
      });
    }
  }

  closeDetail(): void {
    this.showDetailPanel = false;
    this.selectedTask = null;
  }

  updateTask(): void {
    if (!this.selectedTask || this.isSubmitting) return;

    if (this.selectedTask.dueDate) {
      const todayStr = new Date().toISOString().split('T')[0];
      if (this.selectedTask.dueDate < todayStr) {
        this.customAlert = {
          title: 'Validation Error',
          message: 'Task due date cannot be set in the past.'
        };
        return;
      }
    }

    this.isSubmitting = true;
    this.taskService.updateTask(this.selectedTask.id, this.selectedTask).subscribe({
      next: (updatedTask) => {
        const index = this.tasks.findIndex(t => t.id === updatedTask.id);
<<<<<<< HEAD
        const userFullName = this.currentUser ? `${this.currentUser.firstName} ${this.currentUser.lastName || ''}`.trim() : '';
        
        if (updatedTask.assignedTo !== userFullName && this.currentUser) {
=======
        if (updatedTask.userId !== this.currentUser?.id && this.currentUser) {
>>>>>>> my-local-work
          if (index !== -1) this.tasks.splice(index, 1);
        } else {
          this.processTask(updatedTask);
          if (index !== -1) this.tasks[index] = updatedTask;
        }

        this.closeDetail();
        this.showToast(`✏️ Task \"${updatedTask.title}\" updated successfully`, 'info');
        this.alertService.createAlert(
          `Task Updated: ${updatedTask.title}`,
          `Task \"${updatedTask.title}\" was updated — Status: ${updatedTask.status}, Due: ${updatedTask.dueDate}`,
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
        this.processTask(updatedTask);
        const index = this.tasks.findIndex(t => t.id === task.id);
        if (index !== -1) this.tasks[index] = updatedTask;
        this.showToast(`Status changed to \"${updatedTask.status}\"`, 'success');
        this.alertService.createAlert(
          `Task Status Changed: ${updatedTask.title}`,
          `Status of \"${updatedTask.title}\" was changed to \"${updatedTask.status}\"`,
          updatedTask.status === 'Completed' ? 'SUCCESS' : 'INFO', 'TASK', updatedTask.id
        ).subscribe();
      },
      error: (err) => console.error('Failed to update inline task status', err)
    });
  }

  addTask(): void {
    if (!this.newTask.title || !this.newTask.dueDate || this.isSubmitting) return;

    const todayStr = new Date().toISOString().split('T')[0];
    if (this.newTask.dueDate < todayStr) {
      this.customAlert = {
        title: 'Validation Error',
        message: 'Target date cannot be in the past.'
      };
      return;
    }

    this.isSubmitting = true;
    this.taskService.createTask(this.newTask).subscribe({
      next: (createdTask) => {
<<<<<<< HEAD
        const userFullName = this.currentUser ? `${this.currentUser.firstName} ${this.currentUser.lastName || ''}`.trim() : '';
        if (createdTask.assignedTo === userFullName || !this.currentUser) {
=======
        if (createdTask.userId === this.currentUser?.id || !this.currentUser) {
>>>>>>> my-local-work
          this.tasks.unshift(createdTask);
        }
        this.showAddModal = false;
        this.newTask = this.emptyTask();
        this.showToast(`✅ Task \"${createdTask.title}\" created!`, 'success');
        this.alertService.createAlert(
          `New Task Scheduled: ${createdTask.title}`,
          `A new task \"${createdTask.title}\" was created — Type: ${createdTask.type}, Priority: ${createdTask.priority}, Due: ${createdTask.dueDate}`,
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

  closeAlert(event?: Event): void {
    if (event) event.stopPropagation();
    this.customAlert = null;
  }

  closeAddModal(): void {
    this.showAddModal = false;
    this.newTask = this.emptyTask() as any;
    this.customAlert = null;
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    try {
      // Si c'est juste une date YYYY-MM-DD, on ajoute l'heure locale 00:00 pour éviter le décalage UTC
      let dateObj: Date;
      if (dateStr.length === 10) {
        dateObj = new Date(dateStr + 'T00:00:00');
      } else {
        // Si c'est une chaine ISO complete, on la traite mais on peut aussi forcer l'affichage local
        dateObj = new Date(dateStr);
      }

      return dateObj.toLocaleString('en-GB', { // en-GB utilise naturellement le format 24h
        year: 'numeric', month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit', hour12: false
      }).replace(',', '');
    } catch { return dateStr; }
  }
}
