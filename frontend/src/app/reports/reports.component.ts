import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../equipment/equipment.service';
import { SupplierService } from '../supplier/supplier.service';
import { ShelfService } from '../shelf/shelf.service';
import { Equipment } from '../equipment/equipment.model';
import { Supplier } from '../supplier/supplier.model';
import { Shelf } from '../shelf/shelf.model';
import { forkJoin } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';

interface TopEquipment {
  name: string;
  demandes: number;
  delai: string;
  ruptures: number;
}

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  equipments: Equipment[] = [];
  suppliers: Supplier[] = [];
  shelves: Shelf[] = [];

  isLoading = true;

  // Header Filters
  selectedReport = 'Rapport fournisseurs';
  selectedPeriod = '6 mois';
  
  // KPIs
  kpis = {
    valeurTotale: 0,
    valeurTrend: '+8.7%',
    rotation: '3.2x',
    rotationTrend: '+12.1%',
    satisfaction: '94.5%',
    satisfactionTrend: '+2.3%',
    reductionCouts: '12.8%',
    reductionTrend: '+4.5%'
  };

  topEquipments: TopEquipment[] = [];

  // Evolution du Stock (Line Chart)
  public evolutionOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 6 } }
    },
    scales: {
      y: { position: 'left', ticks: { callback: (val) => val + 'k€' } },
      y1: { position: 'right', grid: { drawOnChartArea: false } }
    },
    elements: {
      line: { tension: 0.4 }
    }
  };
  public evolutionData: ChartConfiguration['data'] = {
    labels: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin'],
    datasets: []
  };

  // Analyse des Coûts (Stacked Area)
  public costsOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 6 } } },
    scales: {
      x: { stacked: true },
      y: { stacked: true, ticks: { callback: (val) => val + 'k€' } }
    },
    elements: { line: { tension: 0.4, fill: true } }
  };
  public costsData: ChartConfiguration['data'] = {
    labels: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin'],
    datasets: []
  };

  // Performance par Catégorie (Bar Chart)
  public perfOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom', labels: { usePointStyle: false, boxWidth: 12 } } },
    scales: {
      x: { grid: { display: false } },
      y: { max: 100 }
    }
  };
  public perfData: ChartConfiguration['data'] = {
    labels: [],
    datasets: []
  };

  // Demandes par Département (Red Bars)
  public demandesOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { display: false } },
      y: { max: 80 }
    }
  };
  public demandesData: ChartConfiguration['data'] = {
    labels: ['IT', 'RH', 'Finance', 'Ventes', 'Marketing', 'Opérations'],
    datasets: [
      { data: [45, 32, 28, 52, 38, 20], backgroundColor: '#dc2626', borderRadius: 4 }
    ]
  };

  constructor(
    private equipmentService: EquipmentService,
    private supplierService: SupplierService,
    private shelfService: ShelfService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    forkJoin({
      equipments: this.equipmentService.getAllEquipment(),
      suppliers: this.supplierService.getAllSuppliers(),
      shelves: this.shelfService.getAllShelves()
    }).subscribe({
      next: (res) => {
        this.equipments = res.equipments;
        this.suppliers = res.suppliers;
        this.shelves = res.shelves;
        
        this.calculateRealBaseMetrics();
        this.generateMockCharts();
        
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading report data', err);
        this.isLoading = false;
      }
    });
  }

  calculateRealBaseMetrics(): void {
    // We calculate what we can accurately, e.g. total value.
    if (this.equipments.length > 0) {
      this.kpis.valeurTotale = this.equipments.reduce((sum, e) => sum + (e.purchasePrice || 0), 0);
    } else {
      this.kpis.valeurTotale = 847320; // Fallback to mockup value if DB is empty
    }
  }

  generateMockCharts(): void {
    // 1. Evolution Globale
    this.evolutionData.datasets = [
      { data: [750, 760, 780, 800, 820, 847], label: 'Valeur du stock', borderColor: '#3b82f6', backgroundColor: 'rgba(59,130,246,0.2)', fill: true, yAxisID: 'y' },
      { data: [110, 115, 130, 125, 140, 155], label: 'Commandes', borderColor: '#22c55e', backgroundColor: 'transparent', borderDash: [5, 5], yAxisID: 'y1' },
      { data: [95, 105, 100, 120, 115, 135], label: 'Demandes', borderColor: '#f59e0b', backgroundColor: 'transparent', yAxisID: 'y1' }
    ];

    // 2. Analyse des Coûts
    this.costsData.datasets = [
      { data: [110, 115, 120, 110, 130, 135], label: 'Achats', borderColor: '#475569', backgroundColor: '#e2e8f0', fill: true },
      { data: [10, 12, 11, 13, 14, 15], label: 'Stockage', borderColor: '#22c55e', backgroundColor: 'rgba(34,197,94,0.5)', fill: true },
      { data: [15, 14, 16, 15, 17, 18], label: 'Logistique', borderColor: '#ca8a04', backgroundColor: '#b45309', fill: true }
    ];

    // 3. Performance par Catégorie
    // Try to get real categories, else fallback
    let cats = [...new Set(this.equipments.map(e => e.category).filter(Boolean) as string[])];
    if (cats.length === 0) cats = ['Laptops', 'Storage', 'Memory', 'Monitors'];
    this.perfData.labels = cats.slice(0, 4);
    this.perfData.datasets = [
      { data: [85, 70, 92, 88], label: 'Taux de rotation', backgroundColor: '#1e3a8a', barPercentage: 0.6, categoryPercentage: 0.5 },
      { data: [95, 82, 98, 90], label: 'Satisfaction %', backgroundColor: '#166534', barPercentage: 0.6, categoryPercentage: 0.5 }
    ];

    // 4. Top 5 List
    this.topEquipments = [
      { name: 'Dell Latitude 5520', demandes: 45, delai: '2.3j', ruptures: 2 },
      { name: 'Samsung 970 EVO 1TB', demandes: 38, delai: '1.2j', ruptures: 0 },
      { name: 'Lenovo ThinkPad X1', demandes: 32, delai: '3.1j', ruptures: 1 },
      { name: 'Dell 24" Monitor P2422H', demandes: 28, delai: '1.5j', ruptures: 0 },
      { name: 'Logitech MX Master 3', demandes: 25, delai: '0.8j', ruptures: 0 }
    ];
  }

  refresh(): void {
    this.loadData();
  }

  exportExcel(): void {
    // Action logic here (can integrate xlsx later)
    alert("Export Excel demandé");
  }

  exportPdf(): void {
    // Action logic here (can integrate jspdf later)
    alert("Export PDF demandé");
  }
}
