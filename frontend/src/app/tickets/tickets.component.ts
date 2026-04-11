import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TicketService } from './ticket.service';
import { AuthService } from '../auth.service';
import { Ticket } from './ticket.model';

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tickets.component.html',
  styleUrl: './tickets.component.css'
})
export class TicketsComponent implements OnInit {
  tickets: Ticket[] = [];
  filteredTickets: Ticket[] = [];
  searchQuery: string = '';
  activeFilter: 'All' | 'Open' | 'Resolved' = 'All';
  showAddModal: boolean = false;
  isSubmitting: boolean = false;
  currentUser: any;

  newTicket: Ticket = {
    title: '',
    description: '',
    category: 'Generic',
    priority: 'Medium',
    status: 'Open'
  };

  constructor(
    private ticketService: TicketService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadTickets();
  }

  loadTickets(): void {
    this.ticketService.getTickets().subscribe({
      next: (data) => {
        this.tickets = data;
        this.applyFilters();
      },
      error: (err) => console.error('Error loading tickets', err)
    });
  }

  applyFilters(): void {
    let result = [...this.tickets];

    if (this.activeFilter === 'Open') {
      result = result.filter(t => t.status === 'Open' || t.status === 'In Progress');
    } else if (this.activeFilter === 'Resolved') {
      result = result.filter(t => t.status === 'Resolved' || t.status === 'Closed');
    }

    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      result = result.filter(t => 
        t.title.toLowerCase().includes(query) || 
        t.description.toLowerCase().includes(query) ||
        t.id?.toLowerCase().includes(query)
      );
    }

    this.filteredTickets = result.sort((a,b) => 
      new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
    );
  }

  createTicket(): void {
    if (!this.newTicket.title || !this.newTicket.description) return;

    this.isSubmitting = true;
    const ticketToCreate = { ...this.newTicket, userId: this.currentUser?.id };
    
    this.ticketService.createTicket(ticketToCreate).subscribe({
      next: (created) => {
        this.tickets.unshift(created);
        this.applyFilters();
        this.closeModal();
        this.isSubmitting = false;
      },
      error: (err) => {
        console.error('Error creating ticket', err);
        this.isSubmitting = false;
      }
    });
  }

  setFilter(filter: 'All' | 'Open' | 'Resolved'): void {
    this.activeFilter = filter;
    this.applyFilters();
  }

  openModal(): void {
    this.showAddModal = true;
    this.newTicket = {
      title: '',
      description: '',
      category: 'Generic',
      priority: 'Medium',
      status: 'Open'
    };
  }

  closeModal(): void {
    this.showAddModal = false;
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'High': return 'prio-high';
      case 'Medium': return 'prio-medium';
      case 'Low': return 'prio-low';
      default: return '';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Open': return 'status-open';
      case 'In Progress': return 'status-progress';
      case 'Resolved': return 'status-resolved';
      case 'Closed': return 'status-closed';
      default: return '';
    }
  }
}
