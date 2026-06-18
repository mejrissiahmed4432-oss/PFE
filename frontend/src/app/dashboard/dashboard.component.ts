import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardStats } from './dashboard.service';
import { SoftwareService } from '../software-management/software.service';
import { Software } from '../software-management/software.model';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { RefreshService } from '../shared/refresh.service';
import { Subscription } from 'rxjs';
import { AuthService } from '../auth.service';
import { TicketService } from '../tickets/ticket.service';
import { ProcurementService } from '../procurement/procurement.service';
import { TaskService } from '../task-management/task.service';
import { AlertService } from '../alerts/alert.service';
import { UserService } from '../user-management/user.service';
import { DepartmentService } from '../admin-dashboard/department.service';
import { EmployeeService } from '../employee/employee.service';
import { CategoryService } from '../category-manager/category.service';
import { ShelfService } from '../shelf/shelf.service';
import { SupplierService } from '../supplier/supplier.service';
import { EquipmentService } from '../equipment/equipment.service';
import { PersonalRequestService } from '../personal-requests/personal-request.service';
import { PartRequestService } from '../parts-management/part-request.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  stats: DashboardStats | null = null;
  softwareList: Software[] = [];
  isLoading = true;
  private refreshSubscription?: Subscription;

  userRole: string = 'USER';
  userId: string = '';

  // Specific role stats mapped to variables
  pendingProcurementCount = 0;
  activeTicketsCount = 0;
  myTickets: any[] = [];
  procurementRequests: any[] = [];

  // Universal Stats
  totalTasksCount = 0;
  totalTicketsCount = 0;
  criticalEventsCount = 0;
  personalRequestsCount = 0;
  technicianPartsCount = 0; // For technician only
  
  // Tables Data
  recentAlerts: any[] = [];
  recentRequests: any[] = [];
  fullShelves: any[] = [];

  // IT Manager Stats
  suppliersCount = 0;
  equipmentInUseCount = 0;
  equipmentAvailableCount = 0;
  usersCount = 0;

  // Stock Manager Stats
  categoriesCount = 0;
  shelvesCount = 0;
  assetsCount = 0;

  // Admin Stats
  departmentsCount = 0;
  employeesCount = 0;

  // Pie Chart (Category) - Modernized
  public pieChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true, position: 'right', labels: { usePointStyle: true, pointStyle: 'circle', padding: 20, font: { size: 12, weight: 600 } } },
      tooltip: { backgroundColor: 'rgba(255, 255, 255, 0.9)', titleColor: '#1e293b', bodyColor: '#475569', borderColor: '#e2e8f0', borderWidth: 1, padding: 12, boxPadding: 6, usePointStyle: true }
    },
    cutout: '70%'
  };
  public pieChartData: ChartData<'doughnut', number[], string | string[]> = {
    labels: [],
    datasets: [{ data: [], backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'], hoverOffset: 15, borderWidth: 0 }]
  };
  public pieChartType: 'doughnut' = 'doughnut';

  // Area Chart (Stock Trends)
  public barChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    elements: { line: { tension: 0.4, fill: true }, point: { radius: 0 } },
    scales: { x: { grid: { display: false }, ticks: { font: { size: 11 } } }, y: { grid: { color: 'rgba(226, 232, 240, 0.5)' }, ticks: { font: { size: 11 } }, border: { display: false } } },
    plugins: { legend: { display: false }, tooltip: { mode: 'index', intersect: false, backgroundColor: 'rgba(255, 255, 255, 0.9)', titleColor: '#1e293b', bodyColor: '#475569', padding: 12 } }
  };
  public barChartData: ChartData<'line'> = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
    datasets: [
      { data: [150, 230, 180, 290, 200, 310, 240, 350, 280, 400, 320, 450], label: 'Current Year', borderColor: '#3b82f6', backgroundColor: 'rgba(59, 130, 246, 0.1)', borderWidth: 3, fill: true },
      { data: [100, 150, 120, 200, 180, 240, 190, 270, 230, 300, 250, 350], label: 'Previous Year', borderColor: '#94a3b8', backgroundColor: 'transparent', borderWidth: 2, borderDash: [5, 5], fill: false }
    ]
  };
  public barChartType: 'line' = 'line';

  constructor(
    private dashboardService: DashboardService,
    private softwareService: SoftwareService,
    private refreshService: RefreshService,
    private authService: AuthService,
    private ticketService: TicketService,
    private procurementService: ProcurementService,
    private taskService: TaskService,
    private alertService: AlertService,
    private userService: UserService,
    private departmentService: DepartmentService,
    private employeeService: EmployeeService,
    private categoryService: CategoryService,
    private shelfService: ShelfService,
    private supplierService: SupplierService,
    private equipmentService: EquipmentService,
    private personalRequestService: PersonalRequestService,
    private partRequestService: PartRequestService
  ) { }

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      if (user) {
        this.userRole = user.role;
        this.userId = user.id;
        this.loadRoleSpecificData();
      }
    });

    this.loadStats();

    this.refreshSubscription = this.refreshService.refresh$.subscribe(actionType => {
      this.loadStats();
      this.loadRoleSpecificData();
    });
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  loadRoleSpecificData(): void {
    // 1. Universal Stats (Tasks, Tickets, Alerts, Requests)
    this.loadUniversalStats();

    // 2. Original legacy tables logic (My Tickets, Procurement Requests)
    if (this.userRole === 'TECHNICIAN') {
      this.ticketService.getTicketsForTechnician(this.userId).subscribe(tickets => {
        const active = tickets.filter(t => t.status !== 'Completed' && t.status !== 'Closed' && t.status !== 'Resolved' && t.status !== 'Cancelled');
        this.activeTicketsCount = active.length;
        this.myTickets = active.slice(0, 5);
      });
    } else if (this.userRole === 'ADMIN') {
      this.ticketService.getTickets().subscribe(tickets => {
        const active = tickets.filter(t => !['Completed', 'Closed', 'Resolved', 'Cancelled'].includes(t.status));
        this.activeTicketsCount = active.length;
        this.myTickets = active.slice(0, 5);
      });
    } else {
      this.ticketService.getTicketsByUser(this.userId).subscribe(tickets => {
        const active = tickets.filter(t => !['Completed', 'Closed', 'Resolved', 'Cancelled'].includes(t.status));
        this.activeTicketsCount = active.length;
        this.myTickets = active.slice(0, 5);
      });
    }

    if (this.userRole === 'TECHNICIAN' && this.userId) {
      this.partRequestService.getMyRequests(this.userId).subscribe(requests => {
        this.technicianPartsCount = requests.filter(r => r.status === 'PENDING' || r.status === 'APPROVED').length;
      });
    }

    if (['IT_MANAGER', 'STOCK_MANAGER', 'ADMIN'].includes(this.userRole)) {
      this.procurementService.getAllRequests().subscribe(requests => {
        this.procurementRequests = requests.slice(0, 5);
        if (this.userRole === 'IT_MANAGER') {
          this.pendingProcurementCount = requests.filter(r => r.status === 'PENDING_IT_APPROVAL' || r.status === 'APPROVED' || r.status === 'RESPONDED').length;
        } else {
          this.pendingProcurementCount = requests.filter(r => r.status === 'APPROVED' || r.status === 'PENDING_IT_APPROVAL').length;
        }
      });
    }

    // 3. New Advanced Role Stats
    if (this.userRole === 'IT_MANAGER' || this.userRole === 'ADMIN') {
      this.loadItManagerStats();
    }
    if (this.userRole === 'STOCK_MANAGER' || this.userRole === 'ADMIN') {
      this.loadStockManagerStats();
    }
    if (this.userRole === 'ADMIN') {
      this.loadAdminStats();
    }
  }

  loadUniversalStats(): void {
    // Tasks (Not Finished)
    if (['ADMIN', 'IT_MANAGER'].includes(this.userRole)) {
      this.taskService.getAllTasks().subscribe(tasks => {
        this.totalTasksCount = tasks.filter(t => !['Finished', 'Completed', 'Closed'].includes(t.status)).length;
      });
    } else {
      this.taskService.getTasksAssignedToUser(this.userId).subscribe(tasks => {
        this.totalTasksCount = tasks.filter(t => !['Finished', 'Completed', 'Closed'].includes(t.status)).length;
      });
    }

    // Tickets
    if (['ADMIN', 'IT_MANAGER', 'STOCK_MANAGER'].includes(this.userRole)) {
      this.ticketService.getTickets().subscribe(tickets => this.totalTicketsCount = tickets.length);
    } else if (this.userRole === 'TECHNICIAN') {
      this.ticketService.getTicketsForTechnician(this.userId).subscribe(tickets => this.totalTicketsCount = tickets.length);
    } else {
      this.ticketService.getTicketsByUser(this.userId).subscribe(tickets => this.totalTicketsCount = tickets.length);
    }

    // Critical Alerts
    this.alertService.getAlerts(this.userId, this.userRole).subscribe(alerts => {
      this.criticalEventsCount = alerts.filter((a: any) => a.status !== 'READ').length;
      this.recentAlerts = alerts.slice(0, 5);
    });

    // Personal Requests
    if (this.userId) {
      this.personalRequestService.getMyRequests(this.userId).subscribe(requests => {
        this.personalRequestsCount = requests.filter(r =>
          r.status === 'PENDING' || r.status === 'UNDER_REVIEW'
        ).length;
        this.recentRequests = requests.slice(0, 5);
      });
    }
  }

  loadItManagerStats(): void {
    this.supplierService.getAllSuppliers().subscribe(suppliers => this.suppliersCount = suppliers.length);
    this.userService.getAllUsers().subscribe(users => this.usersCount = users.length);
    
    this.equipmentService.getAllEquipment().subscribe(equipments => {
       this.equipmentInUseCount = equipments.filter(e => e.status === 'In Use' || e.status === 'Assigned').length;
       this.equipmentAvailableCount = equipments.filter(e => e.status === 'Available' || e.status === 'In Stock').length;
    });
  }

  loadStockManagerStats(): void {
    this.categoryService.getAllCategories().subscribe(categories => this.categoriesCount = categories.length);
    this.shelfService.getAllShelves().subscribe(shelves => {
      this.shelvesCount = shelves.length;
      // Filter shelves that are at or above max capacity, or marked as 'Full'
      this.fullShelves = shelves.filter(s => 
        (s.currentQte >= s.maxQte) || 
        (s.status && s.status.toLowerCase() === 'full')
      ).slice(0, 5);
    });
    
    this.equipmentService.getAllEquipment().subscribe(equipments => {
       this.assetsCount = equipments.filter(e => !this.isConsumable(e)).length;
    });
  }

  private isConsumable(eq: any): boolean {
    if (eq.category === 'Consumable') return true;
    if (!eq.type) return false;
    const t = eq.type.toLowerCase();
    return ['ram', 'hdd', 'ssd', 'cable', 'battery', 'mouse', 'keyboard', 'headset', 'hard drive'].some(term => t.includes(term));
  }

  loadAdminStats(): void {
    this.departmentService.getAll().subscribe(departments => this.departmentsCount = departments.length);
    this.employeeService.getAllEmployees().subscribe(employees => this.employeesCount = employees.length);
    // Users count is already fetched in loadItManagerStats since Admin calls both
  }

  loadStats(): void {
    this.isLoading = true;
    this.dashboardService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.updateCharts(data);
        this.loadSoftware();
      },
      error: (err) => {
        console.error('Error loading dashboard stats:', err);
        this.isLoading = false;
      }
    });
  }

  loadSoftware(): void {
    this.softwareService.getAllSoftware().subscribe({
      next: (list) => {
        this.softwareList = list.slice(0, 5);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading software:', err);
        this.isLoading = false;
      }
    });
  }

  updateCharts(stats: DashboardStats): void {
    const categories = Object.keys(stats.equipmentByCategory);
    const catCounts = Object.values(stats.equipmentByCategory);
    this.pieChartData.labels = categories.length > 0 ? categories : ['No Data'];
    this.pieChartData.datasets[0].data = catCounts.length > 0 ? catCounts : [0];

    const locations = Object.keys(stats.equipmentByLocation);
    const locCounts = Object.values(stats.equipmentByLocation);
    
    if (locations.length > 0) {
      this.barChartData.labels = locations;
      this.barChartData.datasets[0].data = locCounts;
      this.barChartData.datasets[1].data = locCounts.map(v => v * 0.8); 
    }
  }
}
