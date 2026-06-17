import {
  Component, EventEmitter, Input, Output,
  ViewChild, ElementRef, AfterViewChecked, OnInit, OnChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService, AiResponse, Conversation, ChatMessage, ConversationTurn } from './ai.service';
import { AuthService } from '../auth.service';
import { RefreshService } from '../shared/refresh.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.css'
})
export class AiAssistantComponent implements OnInit, AfterViewChecked, OnChanges {
  @Input() isOpen: boolean = false;
  @Output() close = new EventEmitter<void>();

  ngOnChanges(changes: any): void {
    if (changes.isOpen) {
      console.log('AiAssistantComponent: isOpen changed to', changes.isOpen.currentValue);
    }
  }
  @ViewChild('chatBody') private chatBody!: ElementRef;
  @ViewChild('chatInput') private chatInput!: ElementRef;

  newMessage: string = '';
  isLoading: boolean = false;
  isSidebarOpen: boolean = true;
  editingMessageId: number | null = null;

  conversations: Conversation[] = [];
  activeConversation: Conversation | null = null;
  pendingAction: { type: string, payload: any, summary: string } | null = null;
  showConfirmModal: boolean = false;

  // Image Upload State
  selectedImage: string | null = null;
  selectedImageFile: File | null = null;

  get messages(): ChatMessage[] {
    return this.activeConversation?.messages ?? [];
  }

  get currentRole(): string {
    return this.authService.getCurrentUser()?.role || '';
  }

  // Role-specific welcome messages & suggestions
  get WELCOME_MESSAGE(): ChatMessage {
    const role = this.currentRole.toUpperCase();

    let welcomeText = "Hello! I'm your AI Assistant. I can help you manage your work efficiently.";
    let suggestions: string[] = [];

    if (role === 'STOCK_MANAGER' || role === 'IT_MANAGER') {
      welcomeText = "Hello! I'm your AI Assistant. I can help you manage inventory, suppliers, equipment, and handle part requests.";
      suggestions = [
        'Show me low stock items',
        'Add a new laptop Dell XPS',
        'How many suppliers do I have?',
        'Approve pending part requests'
      ];
    } else if (role === 'TECHNICIAN') {
      welcomeText = "Hello! I'm your AI Assistant. I can help you with your tickets, spare parts, and maintenance guidance.";
      suggestions = [
        'Show my assigned tickets',
        'Request a 16GB RAM spare part',
        'How do I replace a laptop battery?',
        'What parts are available for me?'
      ];
    } else {
      suggestions = [
        'How many suppliers do I have?',
        'Which is better: i5 or i7?',
        'Show me low stock items',
        'What is my current stock status?'
      ];
    }

    return {
      id: 0,
      text: welcomeText,
      sender: 'assistant',
      timestamp: new Date(),
      suggestions
    };
  }

  constructor(
    private aiService: AiService,
    private authService: AuthService,
    private refreshService: RefreshService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.isLoading = true;
    this.aiService.getAllConversations().subscribe({
      next: (convs) => {
        this.conversations = convs || [];
        if (this.conversations.length > 0) {
          this.activeConversation = this.conversations[0];
        } else {
          this.startNewConversation();
        }
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.startNewConversation();
      }
    });
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
    this.aiService.saveConversation(conv).subscribe();
    this.editingMessageId = null;
    setTimeout(() => this.resetTextareaHeight());
  }

  loadConversation(conv: Conversation): void {
    this.activeConversation = conv;
    this.editingMessageId = null;
  }

  deleteConversation(conv: Conversation, event: Event): void {
    event.stopPropagation();
    this.aiService.deleteConversation(conv.id).subscribe({
      next: () => {
        this.conversations = this.conversations.filter(c => c.id !== conv.id);
        if (this.activeConversation?.id === conv.id) {
          if (this.conversations.length > 0) {
            this.activeConversation = this.conversations[0];
          } else {
            this.startNewConversation();
          }
        }
      }
    });
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
        this.aiService.saveConversation(this.activeConversation).subscribe();
      }
    }
  }

  // ── Image Upload ────────────────────────────────────────────────────────

  triggerFileInput(): void {
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = 'image/*';
    fileInput.onchange = (e: any) => {
      const file = e.target.files[0];
      if (file) {
        this.selectedImageFile = file;
        const reader = new FileReader();
        reader.onload = (event: any) => {
          this.selectedImage = event.target.result;
          setTimeout(() => this.resetTextareaHeight());
        };
        reader.readAsDataURL(file);
      }
    };
    fileInput.click();
  }

  removeSelectedImage(): void {
    this.selectedImage = null;
    this.selectedImageFile = null;
    setTimeout(() => this.resetTextareaHeight());
  }

  // ── Sending Messages ──────────────────────────────────────────────────────

  sendPrompt(prompt: string): void {
    this.newMessage = prompt;
    this.sendMessage();
  }

  sendMessage(): void {
    if ((!this.newMessage.trim() && !this.selectedImage) || this.isLoading || !this.activeConversation) return;

    const userText = this.newMessage.trim();
    const uploadedImage = this.selectedImage;
    const base64Image = uploadedImage ? uploadedImage.split(',')[1] : undefined;

    this.newMessage = '';
    this.selectedImage = null;
    this.selectedImageFile = null;
    this.editingMessageId = null;

    // Add user message
    const userMsg: ChatMessage = {
      id: Date.now(),
      text: userText,
      sender: 'user',
      timestamp: new Date(),
      imageUrl: uploadedImage || undefined
    };
    this.activeConversation.messages.push(userMsg);
    this.resetTextareaHeight();

    // Auto-title from first user message
    if (this.activeConversation.title === 'New Conversation') {
      this.activeConversation.title = userText.length > 40
        ? userText.slice(0, 40) + '…'
        : userText || 'Image Upload';
    }

    this.aiService.saveConversation(this.activeConversation).subscribe();

    // Build conversation history for the backend (last 10 turns)
    const history: ConversationTurn[] = this.activeConversation.messages
      .slice(-11, -1) // last 10 messages before the current one
      .map(m => ({ role: m.sender, content: m.text }));

    this.isLoading = true;

    this.aiService.query(userText || 'Analyze this image', history, base64Image).subscribe({
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
          isError: !response.success,
          actionPending: response.actionPending,
          actionType: response.actionType,
          actionPayload: response.actionPayload
        };

        if (response.actionPending) {
          this.pendingAction = {
            type: response.actionType!,
            payload: response.actionPayload,
            summary: response.answer
          };
        }
        this.activeConversation!.messages.push(aiMsg);
        this.aiService.saveConversation(this.activeConversation!).subscribe();
        
        // Re-fetch all to ensure order / titles are up to date from backend if we want, but local state is fine.
        // this.aiService.getAllConversations().subscribe(c => this.conversations = c || []);
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
        this.aiService.saveConversation(this.activeConversation!).subscribe();
      }
    });
  }

  // ── Two-step Action Confirmation ─────────────────────────────────────────

  /** Step 1: user clicks the in-chat "Confirm" button → show modal */
  requestConfirmation(): void {
    if (!this.pendingAction) return;
    this.showConfirmModal = true;
  }

  /** Step 2: user clicks "Confirm Action" in the modal → actually execute */
  confirmAction(): void {
    if (!this.pendingAction || !this.activeConversation) return;
    this.showConfirmModal = false;

    this.isLoading = true;
    const action = this.pendingAction;
    this.pendingAction = null;

    // Remove pending flag from last message
    const lastMsg = this.activeConversation.messages[this.activeConversation.messages.length - 1];
    if (lastMsg) lastMsg.actionPending = false;

    this.aiService.executeAction(action.type, action.payload).subscribe({
      next: (response: AiResponse) => {
        this.isLoading = false;

        const aiMsg: ChatMessage = {
          id: Date.now(),
          text: response.answer,
          sender: 'assistant',
          timestamp: new Date(),
          isError: !response.success
        };

        this.activeConversation!.messages.push(aiMsg);
        this.aiService.saveConversation(this.activeConversation!).subscribe();

        // Trigger global refresh so other components update their data automatically
        this.refreshService.triggerRefresh(action.type);

        // Show on-screen toast notification based on actual execution success
        if (response.success) {
          this.toastService.success(`Action Executed: ${this.getActionLabel(action.type)}`);
        } else {
          this.toastService.error(`Action Failed: ${this.getActionLabel(action.type)}`);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.toastService.error("Communication error with AI Service.");
        console.error("AI Action Error:", err);
        this.activeConversation!.messages.push({
          id: Date.now(),
          text: "Sorry, I couldn't execute that action. There might be a connection issue.",
          sender: 'assistant',
          timestamp: new Date(),
          isError: true
        });
        this.aiService.saveConversation(this.activeConversation!).subscribe();
      }
    });
  }

  /** Close modal without executing */
  cancelConfirmModal(): void {
    this.showConfirmModal = false;
  }

  /** Cancel the pending action entirely (in-chat cancel button) */
  cancelAction(): void {
    if (!this.activeConversation) return;
    this.pendingAction = null;
    this.showConfirmModal = false;

    // Clear pending flag from last message
    const lastMsg = this.activeConversation.messages[this.activeConversation.messages.length - 1];
    if (lastMsg) lastMsg.actionPending = false;

    this.activeConversation.messages.push({
      id: Date.now(),
      text: "Action cancelled. Let me know if you need anything else.",
      sender: 'assistant',
      timestamp: new Date()
    });
    this.aiService.saveConversation(this.activeConversation).subscribe();
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

  /** Returns a user-friendly label for the action type */
  getActionLabel(actionType: string): string {
    const labels: Record<string, string> = {
      'ADD_EQUIPMENT': 'Add Equipment',
      'UPDATE_EQUIPMENT': 'Update Equipment',
      'DELETE_EQUIPMENT': 'Delete Equipment',
      'APPROVE_REQUEST': 'Approve Request',
      'REJECT_REQUEST': 'Reject Request',
      'SUBMIT_PART_REQUEST': 'Submit Part Request',
      'CREATE_TASK': 'Create Task',
      'UPDATE_TICKET': 'Update Ticket'
    };
    return labels[actionType] || actionType;
  }

  private scrollToBottom(): void {
    try {
      if (this.chatBody?.nativeElement) {
        this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      }
    } catch { }
  }

  adjustTextareaHeight(event: any): void {
    const textarea = event.target;
    textarea.style.height = 'auto';
    textarea.style.height = (textarea.scrollHeight) + 'px';
  }

  private resetTextareaHeight(): void {
    if (this.chatInput?.nativeElement) {
      this.chatInput.nativeElement.style.height = 'auto';
    }
  }
}