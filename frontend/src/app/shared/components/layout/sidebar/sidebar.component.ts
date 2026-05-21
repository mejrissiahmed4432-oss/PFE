import { Component, EventEmitter, Input, Output, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TicketService } from '../../../../tickets/ticket.service';
import { RefreshService } from '../../../refresh.service';
import { Subscription } from 'rxjs';

import { TranslatePipe } from '../../../translate.pipe';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  @Input() user: any;
  @Input() isSidebarCollapsed: boolean = false;
  @Input() activeTab: string = 'dashboard';
  @Input() selectedNatureFilter: string = '';
  @Input() selectedResourceFilter: string = '';

  @Output() toggleSidebar = new EventEmitter<void>();
  @Output() tabChange = new EventEmitter<string>();
  @Output() natureFilterChange = new EventEmitter<string>();
  @Output() resourceFilterChange = new EventEmitter<string>();

  activeTicketCount: number = 0;
  private refreshSub?: Subscription;

  constructor(
    private ticketService: TicketService,
    private refreshService: RefreshService
  ) {}

  ngOnInit() {
    this.loadTicketCount();
    this.refreshSub = this.refreshService.refresh$.subscribe(() => {
      this.loadTicketCount();
    });
  }

  ngOnDestroy() {
    this.refreshSub?.unsubscribe();
  }

  private loadTicketCount() {
    if (this.user?.role === 'TECHNICIAN') {
      this.ticketService.getTickets().subscribe(tickets => {
        const assigned = tickets.filter(t => t.assignedTo === this.user.id && t.status !== 'Completed');
        this.activeTicketCount = assigned.length;
      });
    }
  }

  onToggleSidebar() {
    this.toggleSidebar.emit();
  }

  onTabChange(tab: string) {
    this.tabChange.emit(tab);
  }

  onNatureFilterChange(filter: string) {
    this.natureFilterChange.emit(filter);
  }

  onResourceFilterChange(filter: string) {
    this.resourceFilterChange.emit(filter);
  }
}
