import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardStats } from './dashboard.service';
import { SoftwareService } from '../software-management/software.service';
import { Software } from '../software-management/software.model';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { RefreshService } from '../shared/refresh.service';
import { Subscription } from 'rxjs';

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

  // Pie Chart (Category) - Modernized
  public pieChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'right',
        labels: {
          usePointStyle: true,
          pointStyle: 'circle',
          padding: 20,
          font: { size: 12, weight: 600 }
        }
      },
      tooltip: {
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        titleColor: '#1e293b',
        bodyColor: '#475569',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        padding: 12,
        boxPadding: 6,
        usePointStyle: true
      }
    },
    cutout: '70%' // Makes it a sleek Donut chart
  };
  public pieChartData: ChartData<'doughnut', number[], string | string[]> = {
    labels: [],
    datasets: [{
      data: [],
      backgroundColor: [
        '#3b82f6', // blue
        '#10b981', // emerald
        '#f59e0b', // amber
        '#ef4444', // red
        '#8b5cf6', // violet
        '#06b6d4'  // cyan
      ],
      hoverOffset: 15,
      borderWidth: 0
    }]
  };
  public pieChartType: 'doughnut' = 'doughnut';

  // Area Chart (Stock Trends) - New style
  public barChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    elements: {
      line: { tension: 0.4, fill: true },
      point: { radius: 0 }
    },
    scales: {
      x: { grid: { display: false }, ticks: { font: { size: 11 } } },
      y: { 
        grid: { color: 'rgba(226, 232, 240, 0.5)' },
        ticks: { font: { size: 11 } },
        border: { display: false }
      }
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        mode: 'index',
        intersect: false,
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        titleColor: '#1e293b',
        bodyColor: '#475569',
        padding: 12
      }
    }
  };
  public barChartData: ChartData<'line'> = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
    datasets: [
      {
        data: [150, 230, 180, 290, 200, 310, 240, 350, 280, 400, 320, 450],
        label: 'Current Year',
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        borderWidth: 3,
        fill: true
      },
      {
        data: [100, 150, 120, 200, 180, 240, 190, 270, 230, 300, 250, 350],
        label: 'Previous Year',
        borderColor: '#94a3b8',
        backgroundColor: 'transparent',
        borderWidth: 2,
        borderDash: [5, 5],
        fill: false
      }
    ]
  };
  public barChartType: 'line' = 'line';

  // Mini Bar Chart (Active Assets)
  public miniBarChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: { display: false },
      y: { display: false }
    },
    plugins: {
      legend: { display: false },
      tooltip: { enabled: false }
    }
  };
  public miniBarChartData: ChartData<'bar'> = {
    labels: Array(15).fill(''),
    datasets: [{
      data: [30, 45, 25, 60, 40, 70, 35, 50, 20, 80, 45, 65, 30, 55, 40],
      backgroundColor: '#ffffff',
      borderRadius: 4,
      barThickness: 6
    }]
  };
  public miniBarChartType: 'bar' = 'bar';

  constructor(
    private dashboardService: DashboardService,
    private softwareService: SoftwareService,
    private refreshService: RefreshService
  ) { }
  
  ngOnInit(): void {
    this.loadStats();

    // Listen for global refresh events (e.g., from AI Assistant)
    this.refreshSubscription = this.refreshService.refresh$.subscribe(actionType => {
      console.log('DashboardComponent: Refreshing stats due to action:', actionType);
      this.loadStats();
    });
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  loadStats(): void {
    this.isLoading = true;
    this.dashboardService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.updateCharts(data);
        this.loadSoftware(); // Fetch software as well
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
        this.softwareList = list.slice(0, 5); // Just show top 5
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading software for dashboard:', err);
        this.isLoading = false;
      }
    });
  }

  updateCharts(stats: DashboardStats): void {
    // Update Pie Chart (Category Distribution)
    const categories = Object.keys(stats.equipmentByCategory);
    const catCounts = Object.values(stats.equipmentByCategory);
    this.pieChartData.labels = categories.length > 0 ? categories : ['No Data'];
    this.pieChartData.datasets[0].data = catCounts.length > 0 ? catCounts : [0];

    // Update Area Chart (Stock Trends - using location data as a proxy for diversity)
    const locations = Object.keys(stats.equipmentByLocation);
    const locCounts = Object.values(stats.equipmentByLocation);
    
    // We'll simulate a trend line if we only have current data, 
    // or just show the distribution across locations
    if (locations.length > 0) {
      this.barChartData.labels = locations;
      this.barChartData.datasets[0].data = locCounts;
      // Hide second dataset if not needed or simulate it
      this.barChartData.datasets[1].data = locCounts.map(v => v * 0.8); 
    }

    // Update Mini Bar Chart (Active Assets breakdown)
    if (locCounts.length > 0) {
      this.miniBarChartData.datasets[0].data = locCounts;
      this.miniBarChartData.labels = locations;
    }
  }
}
