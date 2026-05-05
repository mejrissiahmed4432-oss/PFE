import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HrDashboardService } from './hr-dashboard.service';
import { HrDashboardStats } from './hr-dashboard.model';

@Component({
  selector: 'app-hr-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hr-dashboard.component.html',
  styleUrl: './hr-dashboard.component.css'
})
export class HrDashboardComponent implements OnInit {
  stats: HrDashboardStats | null = null;
  isLoading = true;
  departmentEntries: { name: string; count: number; percent: number }[] = [];

  readonly DEPT_COLORS: { [key: string]: string } = {
    'HR':          '#6366f1',
    'IT':          '#3b82f6',
    'Finance':     '#10b981',
    'Engineering': '#f59e0b',
    'Marketing':   '#ec4899',
    'Sales':       '#14b8a6',
    'Operations':  '#8b5cf6',
  };

  constructor(private hrService: HrDashboardService) {}

  ngOnInit(): void {
    this.hrService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.buildDepartmentEntries(data);
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  buildDepartmentEntries(data: HrDashboardStats): void {
    const total = data.totalEmployees || 1;
    this.departmentEntries = Object.entries(data.employeesByDepartment || {})
      .map(([name, count]) => ({
        name,
        count,
        percent: Math.round((count / total) * 100)
      }))
      .sort((a, b) => b.count - a.count);
  }

  getDeptColor(name: string): string {
    return this.DEPT_COLORS[name] || '#64748b';
  }

  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }

  today(): string {
    return new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  }
}
