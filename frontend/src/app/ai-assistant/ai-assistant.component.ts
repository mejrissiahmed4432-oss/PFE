import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.css'
})
export class AiAssistantComponent {
  @Input() isOpen: boolean = false;
  @Output() close = new EventEmitter<void>();

  onClose() {
    this.close.emit();
  }
}
