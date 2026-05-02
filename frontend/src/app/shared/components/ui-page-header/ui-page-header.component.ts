import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-page-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ui-page-header.component.html',
  styles: ``
})
export class UiPageHeaderComponent {
  @Input() title: string = '';
  @Input() description: string = '';
}
