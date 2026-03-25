import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardStats } from './dashboard.service';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  isLoading = true;

  // Pie Chart (Category)
  public pieChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    plugins: {
      legend: { display: true, position: 'right' },
    }
  };
  public pieChartData: ChartData<'pie', number[], string | string[]> = {
    labels: [],
    datasets: [{ data: [], backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40'] }]
  };
  public pieChartType: ChartType = 'pie';

  // Bar Chart (Location)
  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    scales: {
      x: {},
      y: { min: 0 }
    },
    plugins: {
      legend: { display: false }
    }
  };
  public barChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{ data: [], label: 'Equipments', backgroundColor: '#36A2EB' }]
  };
  public barChartType: ChartType = 'bar';

  constructor(private dashboardService: DashboardService) { }

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.isLoading = true;
    this.dashboardService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.updateCharts(data);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading dashboard stats:', err);
        this.isLoading = false;
      }
    });
  }

  updateCharts(stats: DashboardStats): void {
    // Update Pie Chart
    const categories = Object.keys(stats.equipmentByCategory);
    const catCounts = Object.values(stats.equipmentByCategory);
    this.pieChartData.labels = categories;
    this.pieChartData.datasets[0].data = catCounts;

    // Update Bar Chart
    const locations = Object.keys(stats.equipmentByLocation);
    const locCounts = Object.values(stats.equipmentByLocation);
    this.barChartData.labels = locations;
    this.barChartData.datasets[0].data = locCounts;
  }
}
