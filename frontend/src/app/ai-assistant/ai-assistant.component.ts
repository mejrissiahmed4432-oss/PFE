import {
  Component, EventEmitter, Input, Output,
  ViewChild, ElementRef, AfterViewChecked, OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService, AiResponse, Conversation, ChatMessage, ConversationTurn } from './ai.service';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.css'
})
export class AiAssistantComponent implements OnInit, AfterViewChecked {
  @Input() isOpen: boolean = false;
  @Output() close = new EventEmitter<void>();
  @ViewChild('chatBody') private chatBody!: ElementRef;

  newMessage: string = '';
  isLoading: boolean = false;
  isSidebarOpen: boolean = true;
  editingMessageId: number | null = null;

  conversations: Conversation[] = [];
  activeConversation: Conversation | null = null;

  get messages(): ChatMessage[] {
    return this.activeConversation?.messages ?? [];
  }

  readonly WELCOME_MESSAGE: ChatMessage = {
    id: 0,
    text: "Hello! I'm your AI Assistant. I can help you with stock levels, suppliers, equipment comparisons, maintenance tasks, and much more. Ask me anything!",
    sender: 'assistant',
    timestamp: new Date(),
    suggestions: [
      'How many suppliers do I have?',
      'Which is better: i5 or i7?',
      'Show me low stock items',
      'What is my current stock status?'
    ]
  };

  constructor(private aiService: AiService) {}

  ngOnInit(): void {
    this.conversations = this.aiService.getAllConversations();
    if (this.conversations.length > 0) {
      this.activeConversation = this.conversations[0];
    } else {
      this.startNewConversation();
    }
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  onClose(): void {
    this.close.emit();
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  // ── Conversation Management ───────────────────────────────────────────────

  startNewConversation(): void {
    const conv = this.aiService.createConversation();
    this.activeConversation = conv;
    this.conversations.unshift(conv);
    this.aiService.saveConversation(conv);
    this.editingMessageId = null;
  }

  loadConversation(conv: Conversation): void {
    this.activeConversation = conv;
    this.editingMessageId = null;
  }

  deleteConversation(conv: Conversation, event: Event): void {
    event.stopPropagation();
    this.aiService.deleteConversation(conv.id);
    this.conversations = this.conversations.filter(c => c.id !== conv.id);
    if (this.activeConversation?.id === conv.id) {
      if (this.conversations.length > 0) {
        this.activeConversation = this.conversations[0];
      } else {
        this.startNewConversation();
      }
    }
  }

  // ── Message Editing ───────────────────────────────────────────────────────

  editMessage(msg: ChatMessage): void {
    this.newMessage = msg.text;
    this.editingMessageId = msg.id;
    // Remove all messages from this point onwards
    if (this.activeConversation) {
      const idx = this.activeConversation.messages.findIndex(m => m.id === msg.id);
      if (idx >= 0) {
        this.activeConversation.messages = this.activeConversation.messages.slice(0, idx);
        this.aiService.saveConversation(this.activeConversation);
      }
    }
  }

  // ── Sending Messages ──────────────────────────────────────────────────────

  sendPrompt(prompt: string): void {
    this.newMessage = prompt;
    this.sendMessage();
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || this.isLoading || !this.activeConversation) return;

    const userText = this.newMessage.trim();
    this.newMessage = '';
    this.editingMessageId = null;

    // Add user message
    const userMsg: ChatMessage = {
      id: Date.now(),
      text: userText,
      sender: 'user',
      timestamp: new Date()
    };
    this.activeConversation.messages.push(userMsg);

    // Auto-title from first user message
    if (this.activeConversation.title === 'New Conversation') {
      this.activeConversation.title = userText.length > 40
        ? userText.slice(0, 40) + '…'
        : userText;
    }

    this.aiService.saveConversation(this.activeConversation);

    // Build conversation history for the backend (last 10 turns)
    const history: ConversationTurn[] = this.activeConversation.messages
      .slice(-11, -1) // last 10 messages before the current one
      .map(m => ({ role: m.sender, content: m.text }));

    this.isLoading = true;

    this.aiService.query(userText, history).subscribe({
      next: (response: AiResponse) => {
        this.isLoading = false;

        const aiMsg: ChatMessage = {
          id: Date.now() + 1,
          text: response.success
            ? response.answer
            : (response.errorMessage || 'Sorry, I encountered an error.'),
          sender: 'assistant',
          timestamp: new Date(),
          suggestions: response.success ? response.suggestions : undefined,
          data: response.success ? response.data : undefined,
          isError: !response.success
        };

        this.activeConversation!.messages.push(aiMsg);
        this.aiService.saveConversation(this.activeConversation!);
        this.conversations = this.aiService.getAllConversations();
      },
      error: () => {
        this.isLoading = false;
        this.activeConversation!.messages.push({
          id: Date.now() + 1,
          text: "I'm having trouble connecting to the AI server. Please try again later.",
          sender: 'assistant',
          timestamp: new Date(),
          isError: true
        });
        this.aiService.saveConversation(this.activeConversation!);
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  getConversationPreview(conv: Conversation): string {
    const last = conv.messages.filter(m => m.sender === 'user').pop();
    return last ? (last.text.length > 35 ? last.text.slice(0, 35) + '…' : last.text) : 'No messages yet';
  }

  formatDataValue(value: any): string {
    if (value === null || value === undefined) return 'N/A';
    if (typeof value === 'object') return JSON.stringify(value);
    return String(value);
  }

  private scrollToBottom(): void {
    try {
      if (this.chatBody?.nativeElement) {
        this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      }
    } catch {}
  }
}
