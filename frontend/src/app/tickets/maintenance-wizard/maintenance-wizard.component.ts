import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Equipment } from '../../equipment/equipment.model';

@Component({
  selector: 'app-maintenance-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './maintenance-wizard.component.html',
  styleUrl: './maintenance-wizard.component.css'
})
export class MaintenanceWizardComponent {
  @Output() close = new EventEmitter<void>();
  @Output() select = new EventEmitter<Equipment>();

  currentTab: 'scan' | 'search' | 'browse' = 'scan';
  searchQuery: string = '';
  
  // Browsing filters
  filterCategory: string = 'All Categories';
  filterType: string = 'All Types';
  filterStatus: string = 'All Statuses';

  // Mock Equipment Data
  mockEquipment: Equipment[] = [
    {
      id: '1',
      name: 'Dell Latitude 5520',
      model: 'Latitude 5520',
      serialNumber: 'DL5520-2024-001',
      location: 'Floor 3, Desk 42',
      status: 'active',
      type: 'Laptop',
      category: 'PC'
    },
    {
      id: '2',
      name: 'HP EliteBook 840',
      model: 'EliteBook 840 G8',
      serialNumber: 'HP840-2025-089',
      location: 'Floor 2, Desk 18',
      status: 'active',
      type: 'Laptop',
      category: 'PC'
    },
    {
      id: '3',
      name: 'HP LaserJet Pro',
      model: 'LaserJet Pro M404dn',
      serialNumber: 'HPLJ-2024-056',
      location: 'Floor 4, Print Room',
      status: 'active',
      type: 'Laser Printer',
      category: 'Printer'
    },
    {
      id: '4',
      name: 'Dell PowerEdge R740',
      model: 'PowerEdge R740',
      serialNumber: 'DPE-R740-2023-012',
      location: 'Data Center, Rack 8',
      status: 'maintenance',
      type: 'Rack Server',
      category: 'Server'
    }
  ];

  recentEquipment = this.mockEquipment.slice(0, 2);

  get filteredEquipment(): Equipment[] {
    return this.mockEquipment.filter(e => {
      const matchesSearch = !this.searchQuery || 
        e.name?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        e.serialNumber?.toLowerCase().includes(this.searchQuery.toLowerCase());
      
      const matchesCategory = this.filterCategory === 'All Categories' || e.category === this.filterCategory;
      const matchesType = this.filterType === 'All Types' || e.type === this.filterType;
      const matchesStatus = this.filterStatus === 'All Statuses' || e.status === this.filterStatus.toLowerCase();

      return matchesSearch && matchesCategory && matchesType && matchesStatus;
    });
  }

  setTab(tab: 'scan' | 'search' | 'browse'): void {
    this.currentTab = tab;
  }

  onClose(): void {
    this.close.emit();
  }

  selectEquipment(item: Equipment): void {
    this.select.emit(item);
  }

  // Simulation for QR Upload
  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Simulate scanning delay
      setTimeout(() => {
        const randomItem = this.mockEquipment[Math.floor(Math.random() * this.mockEquipment.length)];
        this.selectEquipment(randomItem);
      }, 1000);
    }
  }

  triggerUpload(): void {
    const fileInput = document.getElementById('qr-upload-input') as HTMLInputElement;
    fileInput.click();
  }
}
