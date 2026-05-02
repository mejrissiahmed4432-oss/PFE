import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ui-table.component.html',
  styles: ``
})
export class UiTableComponent {
  @Input() showToolbar: boolean = true;
  @Input() showSearch: boolean = true;
  @Input() showPagination: boolean = true;
  @Input() searchPlaceholder: string = 'Search...';
  @Input() searchValue: string = '';

  @Output() search = new EventEmitter<string>();

  onSearchChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.search.emit(value);
  }
}
