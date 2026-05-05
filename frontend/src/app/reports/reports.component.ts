import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../equipment/equipment.service';
import { SupplierService } from '../supplier/supplier.service';
import { ShelfService } from '../shelf/shelf.service';
<<<<<<< HEAD
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
=======
import { PartRequestService } from '../parts-management/part-request.service';
import { Equipment } from '../equipment/equipment.model';
import { Supplier } from '../supplier/supplier.model';
import { Shelf } from '../shelf/shelf.model';
import { PartRequest } from '../parts-management/part-request.model';
import { forkJoin } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { Chart, ArcElement, DoughnutController, Tooltip, Legend } from 'chart.js';

Chart.register(ArcElement, DoughnutController, Tooltip, Legend);

interface TopEquipment {
  name: string;
  brand: string;
  category: string;
  price: number;
  qte: number;
>>>>>>> my-local-work
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
<<<<<<< HEAD

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
=======
  partRequests: PartRequest[] = [];

  isLoading = true;

  selectedReports: string[] = ['Equipment Report', 'Locations Report', 'Suppliers Report', 'Requests Report'];
  availableReports = ['Equipment Report', 'Locations Report', 'Suppliers Report', 'Requests Report'];
  startDate: string = '';
  endDate: string = '';

  kpis = {
    valeurTotale: 0,
    totalEquipments: 0,
    pendingRequests: 0,
    shelfAlerts: 0
>>>>>>> my-local-work
  };

  topEquipments: TopEquipment[] = [];

<<<<<<< HEAD
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
=======
  // 1. Inventaire par Catégorie
  public categoryOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { x: { grid: { display: false } }, y: { beginAtZero: true, ticks: { stepSize: 1 } } }
  };
  public categoryData: ChartConfiguration['data'] = { labels: [], datasets: [] };

  // 2. Répartition par Type (Doughnut)
  public typeOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8, padding: 16 } } },
    cutout: '65%'
  };
  public typeData: ChartConfiguration<'doughnut'>['data'] = { labels: [], datasets: [] };

  // 3. Statut Demandes (Horizontal Bar)
  public requestStatusOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: { x: { beginAtZero: true, ticks: { stepSize: 1 } }, y: { grid: { display: false } } }
  };
  public requestStatusData: ChartConfiguration['data'] = { labels: [], datasets: [] };

  // 4. Utilisation Étagères (Horizontal Bar)
  public shelfOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: { x: { min: 0, max: 100, ticks: { callback: (val) => val + '%' } }, y: { grid: { display: false } } }
  };
  public shelfData: ChartConfiguration['data'] = { labels: [], datasets: [] };

  // 5. Performance Fournisseurs
  public supplierOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { x: { grid: { display: false } }, y: { min: 0, max: 5, ticks: { stepSize: 1 } } }
  };
  public supplierData: ChartConfiguration['data'] = { labels: [], datasets: [] };
>>>>>>> my-local-work

  constructor(
    private equipmentService: EquipmentService,
    private supplierService: SupplierService,
<<<<<<< HEAD
    private shelfService: ShelfService
  ) {}

  ngOnInit(): void {
=======
    private shelfService: ShelfService,
    private partRequestService: PartRequestService
  ) {}

  ngOnInit(): void {
    const now = new Date();
    this.endDate = now.toISOString().split('T')[0];
    
    const past = new Date();
    past.setMonth(past.getMonth() - 6);
    this.startDate = past.toISOString().split('T')[0];

>>>>>>> my-local-work
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    forkJoin({
      equipments: this.equipmentService.getAllEquipment(),
      suppliers: this.supplierService.getAllSuppliers(),
<<<<<<< HEAD
      shelves: this.shelfService.getAllShelves()
=======
      shelves: this.shelfService.getAllShelves(),
      partRequests: this.partRequestService.getAllRequests()
>>>>>>> my-local-work
    }).subscribe({
      next: (res) => {
        this.equipments = res.equipments;
        this.suppliers = res.suppliers;
        this.shelves = res.shelves;
<<<<<<< HEAD
        
        this.calculateRealBaseMetrics();
        this.generateMockCharts();
        
=======
        this.partRequests = res.partRequests;
        this.buildKPIs();
        this.buildCategoryChart();
        this.buildTypeChart();
        this.buildRequestStatusChart();
        this.buildShelfChart();
        this.buildTopEquipments();
        this.buildSupplierChart();
>>>>>>> my-local-work
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading report data', err);
        this.isLoading = false;
      }
    });
  }

<<<<<<< HEAD
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
=======
  onDateRangeChange(): void {
    if (this.startDate > this.endDate) {
      const temp = this.startDate;
      this.startDate = this.endDate;
      this.endDate = temp;
    }
    this.refreshCharts();
  }

  private refreshCharts(): void {
    if (this.partRequests.length > 0 || this.equipments.length > 0) {
      this.buildKPIs();
      this.buildCategoryChart();
      this.buildTypeChart();
      this.buildRequestStatusChart();
      this.buildTopEquipments();
    }
  }

  private translate(val: string | undefined | null): string {
    if (!val) return 'N/A';
    const mapping: { [key: string]: string } = {
      // Categories
      'Ordinateur': 'Computer',
      'Imprimante': 'Printer',
      'Ecran': 'Monitor',
      'Souris': 'Mouse',
      'Clavier': 'Keyboard',
      'Serveur': 'Server',
      'Réseau': 'Network',
      'Téléphone': 'Phone',
      'Non catégorisé': 'Uncategorized',
      
      // Statuses
      'DISPONIBLE': 'Available',
      'EN_USAGE': 'In Use',
      'EN_MAINTENANCE': 'In Maintenance',
      'REFORME': 'Retired',
      'BASSE': 'Low',
      'MOYENNE': 'Medium',
      'HAUTE': 'High',
      'TERMINEE': 'Completed',
      'ANNULEE': 'Cancelled',
      'PENDING': 'Pending',
      'APPROVED': 'Approved',
      'REJECTED': 'Rejected'
    };
    return mapping[val] || val;
  }

  private getFilteredRequests(): PartRequest[] {
    if (!this.startDate || !this.endDate) return this.partRequests;
    
    const [sy, sm, sd] = this.startDate.split('-');
    const start = new Date(Number(sy), Number(sm) - 1, Number(sd), 0, 0, 0, 0);
    
    const [ey, em, ed] = this.endDate.split('-');
    const end = new Date(Number(ey), Number(em) - 1, Number(ed), 23, 59, 59, 999);
    
    return this.partRequests.filter(r => {
      if (!r.createdAt) return false;
      const d = new Date(r.createdAt); // Converts UTC to local timezone
      return d >= start && d <= end;
    });
  }

  private getFilteredEquipments(): Equipment[] {
    if (!this.startDate || !this.endDate) return this.equipments;
    
    const [sy, sm, sd] = this.startDate.split('-');
    const start = new Date(Number(sy), Number(sm) - 1, Number(sd), 0, 0, 0, 0);
    
    const [ey, em, ed] = this.endDate.split('-');
    const end = new Date(Number(ey), Number(em) - 1, Number(ed), 23, 59, 59, 999);
    
    return this.equipments.filter(e => {
      // Use strictly purchaseDate as requested
      const dateString = e.purchaseDate;
      if (!dateString) return false;
      const d = new Date(dateString); // Converts UTC to local timezone
      return d >= start && d <= end;
    });
  }

  buildKPIs(): void {
    const filteredEq = this.getFilteredEquipments();
    this.kpis.valeurTotale = filteredEq.reduce(
      (sum, e) => sum + ((e.purchasePrice || 0) * (e.qte || 1)), 0
    );
    this.kpis.totalEquipments = filteredEq.length;
    this.kpis.pendingRequests = this.getFilteredRequests().filter(r => r.status === 'PENDING').length;
    this.kpis.shelfAlerts = this.shelves.filter(s => s.currentQte < s.minQte).length;
  }

  buildCategoryChart(): void {
    const catMap = new Map<string, number>();
    this.getFilteredEquipments().forEach(e => {
      const cat = this.translate(e.category || 'Uncategorized');
      catMap.set(cat, (catMap.get(cat) || 0) + 1);
    });
    const sorted = [...catMap.entries()].sort((a, b) => b[1] - a[1]);
    const palette = ['#3b82f6', '#8b5cf6', '#06b6d4', '#f59e0b', '#10b981', '#ef4444', '#ec4899', '#6366f1'];
    this.categoryData = {
      labels: sorted.map(s => s[0]),
      datasets: [{ data: sorted.map(s => s[1]), backgroundColor: sorted.map((_, i) => palette[i % palette.length]), borderRadius: 6, barPercentage: 0.6 }]
    };
  }

  buildTypeChart(): void {
    const typeMap = new Map<string, number>();
    this.getFilteredEquipments().forEach(e => {
      const t = this.translate(e.type || 'Other');
      typeMap.set(t, (typeMap.get(t) || 0) + 1);
    });
    const sorted = [...typeMap.entries()].sort((a, b) => b[1] - a[1]);
    const palette = ['#3b82f6', '#8b5cf6', '#06b6d4', '#f59e0b', '#10b981', '#ef4444', '#ec4899', '#6366f1', '#14b8a6', '#f97316'];
    this.typeData = {
      labels: sorted.map(s => s[0]),
      datasets: [{ data: sorted.map(s => s[1]), backgroundColor: sorted.map((_, i) => palette[i % palette.length]), borderWidth: 2, borderColor: '#ffffff' }]
    };
  }

  buildRequestStatusChart(): void {
    const filtered = this.getFilteredRequests();
    this.requestStatusData = {
      labels: ['Pending', 'Approved', 'Rejected'],
      datasets: [{ data: [
        filtered.filter(r => r.status === 'PENDING').length,
        filtered.filter(r => r.status === 'APPROVED').length,
        filtered.filter(r => r.status === 'REJECTED').length
      ], backgroundColor: ['#f59e0b', '#10b981', '#ef4444'], borderRadius: 6, barPercentage: 0.5 }]
    };
  }

  buildShelfChart(): void {
    const entries = this.shelves.map(s => {
      const pct = s.maxQte > 0 ? Math.round((s.currentQte / s.maxQte) * 100) : 0;
      const color = s.currentQte < s.minQte ? '#ef4444' : pct > 85 ? '#f59e0b' : '#10b981';
      return { label: `Shelf ${s.nb}`, pct, color };
    });
    this.shelfData = {
      labels: entries.map(e => e.label),
      datasets: [{ data: entries.map(e => e.pct), backgroundColor: entries.map(e => e.color), borderRadius: 6, barPercentage: 0.5 }]
    };
  }

  buildTopEquipments(): void {
    this.topEquipments = [...this.getFilteredEquipments()]
      .filter(e => (e.purchasePrice || 0) > 0)
      .sort((a, b) => (b.purchasePrice || 0) - (a.purchasePrice || 0))
      .slice(0, 5)
      .map(e => ({ 
        name: e.equipmentName || e.name || 'N/A', 
        brand: e.brand || 'N/A', 
        category: this.translate(e.category || 'N/A'), 
        price: e.purchasePrice || 0, 
        qte: e.qte || 1 
      }));
  }

  buildSupplierChart(): void {
    const sups = this.suppliers.filter(s => s.rating > 0).sort((a, b) => b.rating - a.rating).slice(0, 8);
    this.supplierData = {
      labels: sups.map(s => s.companyName),
      datasets: [{ data: sups.map(s => s.rating), backgroundColor: sups.map(s => s.rating >= 4 ? '#10b981' : s.rating >= 3 ? '#f59e0b' : '#ef4444'), borderRadius: 6, barPercentage: 0.5 }]
    };
  }

  translateStatus(status: string | undefined): string {
    return this.translate(status);
  }

  showSection(section: string): boolean {
    const map: { [key: string]: string[] } = {
      'Equipment Report': ['category', 'type', 'top5'],
      'Locations Report': ['shelves'],
      'Suppliers Report': ['suppliers'],
      'Requests Report': ['requests']
    };
    
    // Show section if any of the report types that include it are selected
    return this.selectedReports.some(report => (map[report] || []).includes(section));
  }

  isReportSelected(report: string): boolean {
    return this.selectedReports.includes(report);
  }

  toggleReport(report: string): void {
    const index = this.selectedReports.indexOf(report);
    if (index > -1) {
      this.selectedReports.splice(index, 1);
    } else {
      this.selectedReports.push(report);
    }

    // Auto-select All if nothing is selected
    if (this.selectedReports.length === 0) {
      this.selectedReports = [...this.availableReports];
    }
  }

  toggleAllReports(): void {
    if (this.selectedReports.length === this.availableReports.length) {
      this.selectedReports = [];
    } else {
      this.selectedReports = [...this.availableReports];
    }

    // Auto-select All if result is empty
    if (this.selectedReports.length === 0) {
      this.selectedReports = [...this.availableReports];
    }
  }

  refresh(): void { this.loadData(); }

  // ── Excel Export (respects selectedReport) ──
  exportExcel(): void {
    import('xlsx').then(XLSX => {
      const wb = XLSX.utils.book_new();
      const date = new Date().toISOString().slice(0, 10);

      if (this.isReportSelected('Equipment Report')) {
        const data = this.getFilteredEquipments().map(e => ({
          'Name': e.equipmentName || e.name || '',
          'Brand': e.brand || '',
          'Category': this.translate(e.category || ''),
          'Type': this.translate(e.type || ''),
          'Serial N°': e.serialNumber || '',
          'Price (€)': e.purchasePrice || 0,
          'Quantity': e.qte || 1,
          'Status': this.translate(e.status || ''),
          'Supplier': e.supplier || ''
        }));
        XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(data), 'Equipment');
      }

      if (this.isReportSelected('Locations Report')) {
        const data = this.shelves.map(s => ({
          'Shelf': s.nb,
          'Equipment Type': this.translate(s.equipmentType),
          'Current Qty': s.currentQte,
          'Min Qty': s.minQte,
          'Max Qty': s.maxQte,
          'Status': this.translate(s.status),
          '% Usage': s.maxQte > 0 ? Math.round((s.currentQte / s.maxQte) * 100) + '%' : '0%',
          'Alert': s.currentQte < s.minQte ? 'CRITICAL' : 'OK'
        }));
        XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(data), 'Shelves');
      }

      if (this.isReportSelected('Requests Report')) {
        const data = this.getFilteredRequests().map(r => ({
          'Requester': r.requesterName,
          'Status': this.translate(r.status || ''),
          'Priority': this.translate(r.priority),
          'Description': r.description,
          'Date': r.createdAt || '',
          'Item Count': r.items?.length || 0
        }));
        XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(data), 'Requests');
      }

      if (this.isReportSelected('Suppliers Report')) {
        const data = this.suppliers.map(s => ({
          'Company': s.companyName,
          'Contact': s.contactPerson,
          'Email': s.email,
          'Phone': s.phoneNumber,
          'Category': this.translate(s.category),
          'Rating (/5)': s.rating
        }));
        XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(data), 'Suppliers');
      }

      if (wb.SheetNames.length === 0) { alert('No data to export.'); return; }
      XLSX.writeFile(wb, `inventory_report_${date}.xlsx`);
    });
  }

  // ── PDF Export (respects selectedReport, fixes autoTable) ──
  exportPdf(): void {
    const date = new Date().toISOString().slice(0, 10);
    import('jspdf').then(({ default: jsPDF }) => {
      import('jspdf-autotable').then(({ default: autoTable }) => {
        const doc = new jsPDF('l', 'mm', 'a4');

        doc.setFontSize(22);
        doc.setTextColor(15, 23, 42);
        doc.text('IT Inventory Report', 14, 22);
        doc.setFontSize(10);
        doc.setTextColor(100, 116, 139);
        doc.text(`Generated on ${new Date().toLocaleDateString('en-US')} — Period: ${this.startDate} to ${this.endDate}`, 14, 30);
        doc.text(`Selection: ${this.selectedReports.join(', ') || 'None'}`, 14, 36);

        doc.setFontSize(13);
        doc.setTextColor(15, 23, 42);
        doc.text('KPI Summary', 14, 48);
        autoTable(doc, {
          startY: 52,
          head: [['Indicator', 'Value']],
          body: [
            ['Total Stock Value', this.kpis.valeurTotale.toLocaleString('en-US', { style: 'currency', currency: 'EUR' })],
            ['Equipment in Stock', this.kpis.totalEquipments.toString()],
            ['Pending Requests', this.kpis.pendingRequests.toString()],
            ['Shelf Alerts', this.kpis.shelfAlerts.toString()]
          ],
          theme: 'grid',
          headStyles: { fillColor: [30, 58, 138] },
          margin: { left: 14 }
        });

        if (this.isReportSelected('Equipment Report')) {
          doc.addPage();
          doc.setFontSize(14); doc.setTextColor(15, 23, 42);
          doc.text('Equipment Inventory', 14, 20);
          autoTable(doc, {
            startY: 26,
            head: [['Name', 'Brand', 'Category', 'Type', 'Price (€)', 'Qty', 'Status']],
            body: this.getFilteredEquipments().slice(0, 100).map(e => [
              e.equipmentName || e.name || '', e.brand || '', this.translate(e.category),
              this.translate(e.type), (e.purchasePrice || 0).toLocaleString('en-US'), e.qte || 1, this.translate(e.status)
            ]),
            theme: 'striped', headStyles: { fillColor: [30, 58, 138] }, styles: { fontSize: 8 }, margin: { left: 14 }
          });
        }

        if (this.isReportSelected('Locations Report')) {
          doc.addPage();
          doc.setFontSize(14); doc.setTextColor(15, 23, 42);
          doc.text('Shelf Status', 14, 20);
          autoTable(doc, {
            startY: 26,
            head: [['Shelf', 'Type', 'Current', 'Min', 'Max', '% Usage', 'Status']],
            body: this.shelves.map(s => [
              s.nb, this.translate(s.equipmentType), s.currentQte, s.minQte, s.maxQte,
              s.maxQte > 0 ? Math.round((s.currentQte / s.maxQte) * 100) + '%' : '0%',
              s.currentQte < s.minQte ? 'CRITICAL' : 'OK'
            ]),
            theme: 'striped', headStyles: { fillColor: [30, 58, 138] }, styles: { fontSize: 9 }, margin: { left: 14 }
          });
        }

        if (this.isReportSelected('Requests Report')) {
          doc.addPage();
          doc.setFontSize(14); doc.setTextColor(15, 23, 42);
          doc.text(`Part Requests — ${this.startDate} to ${this.endDate}`, 14, 20);
          autoTable(doc, {
            startY: 26,
            head: [['Requester', 'Status', 'Priority', 'Description', 'Date', 'Items']],
            body: this.getFilteredRequests().map(r => [
              r.requesterName, this.translate(r.status), this.translate(r.priority), r.description,
              r.createdAt ? new Date(r.createdAt).toLocaleDateString('en-US') : '',
              r.items?.length || 0
            ]),
            theme: 'striped', headStyles: { fillColor: [30, 58, 138] }, styles: { fontSize: 8 }, margin: { left: 14 }
          });
        }

        if (this.isReportSelected('Suppliers Report')) {
          doc.addPage();
          doc.setFontSize(14); doc.setTextColor(15, 23, 42);
          doc.text('Supplier Performance', 14, 20);
          autoTable(doc, {
            startY: 26,
            head: [['Company', 'Contact', 'Email', 'Phone', 'Category', 'Rating /5']],
            body: this.suppliers.map(s => [s.companyName, s.contactPerson, s.email, s.phoneNumber, this.translate(s.category), s.rating]),
            theme: 'striped', headStyles: { fillColor: [30, 58, 138] }, styles: { fontSize: 9 }, margin: { left: 14 }
          });
        }

        doc.save(`inventory_report_${date}.pdf`);
      });
    });
>>>>>>> my-local-work
  }
}
