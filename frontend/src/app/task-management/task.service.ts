import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TaskItem {
  id: string;
  title: string;
  description: string;
  type: string;
  priority: string;
  status: string;
  dueDate: string;
  assignedByUserId: string;
  assignedUserIds: string[];
  createdAt: string;
  updatedAt: string;
  originalDueDate?: string;
}

export interface TaskAssignRequest {
  title: string;
  description: string;
  priority: string;
  type: string;
  dueDate: string;
  assignedByUserId: string;
  assignedUserIds: string[];
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private apiUrl = '/api/it-manager/tasks';

  constructor(private http: HttpClient) {}

  assignTask(request: TaskAssignRequest): Observable<TaskItem> {
    return this.http.post<TaskItem>(`${this.apiUrl}/assign`, request);
  }

  getAllTasks(): Observable<TaskItem[]> {
    return this.http.get<TaskItem[]>(this.apiUrl);
  }

  getTasksAssignedByManager(managerId: string): Observable<TaskItem[]> {
    return this.http.get<TaskItem[]>(`${this.apiUrl}/assigned-by/${managerId}`);
  }

  getTasksAssignedToUser(userId: string): Observable<TaskItem[]> {
    return this.http.get<TaskItem[]>(`${this.apiUrl}/assigned-to/${userId}`);
  }

  updateTask(id: string, task: any): Observable<TaskItem> {
    return this.http.put<TaskItem>(`${this.apiUrl}/${id}`, task);
  }

  updateTaskStatus(id: string, status: string): Observable<TaskItem> {
    return this.http.patch<TaskItem>(`${this.apiUrl}/${id}/status`, null, {
      params: { status }
    });
  }

  deleteTask(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
