import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { MessagingService } from './messaging.service';
import { SocketService } from './socket.service';
import { Subscription, interval } from 'rxjs';

export interface ChatMessage {
  id?: string;
  senderId: string;
  receiverId: string;
  content: string;
  timestamp: Date;
  status: string;
  attachmentId?: string;
  fileName?: string;
  fileType?: string;
  isOwn?: boolean;
  edited?: boolean;
  deletedForSender?: boolean;
  deletedForReceiver?: boolean;
  deletedForEveryone?: boolean;
}

export interface UserContact {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  photo: string;
  initials: string;
  lastMessage?: string;
  lastTime?: Date;
  unreadCount?: number;
  online?: boolean;
}

@Component({
  selector: 'app-messaging',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messaging.component.html',
  styleUrl: './messaging.component.css'
})
export class MessagingComponent implements OnInit, AfterViewChecked, OnDestroy {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  @ViewChild('fileInput') private fileInput!: ElementRef;

  currentUser: any;
  currentUserInitials: string = '';

  searchQuery: string = '';
  newMessage: string = '';
  selectedUser: UserContact | null = null;
  messages: ChatMessage[] = [];
  contacts: UserContact[] = [];
  
  shouldScrollToBottom: boolean = false;
  editingMessageId: string | null = null;
  editContent: string = '';
  private pollSubscription?: Subscription;
  private socketSubscription?: Subscription;

  // Delete confirmation modal
  showDeleteModal: boolean = false;
  pendingDeleteMsg: ChatMessage | null = null;
  isOwnMessageModal: boolean = false; // true = show both options, false = only "delete for me"

  constructor(
    private authService: AuthService,
    private messagingService: MessagingService,
    private socketService: SocketService
  ) {}

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.currentUser = user || this.authService.getCurrentUser();
      if (this.currentUser) {
        this.currentUserInitials = `${this.currentUser.firstName?.charAt(0) || ''}${this.currentUser.lastName?.charAt(0) || ''}`.toUpperCase() || 'ME';
      }
    });

    this.loadContacts();
    
    // 1. Subscribe to WebSocket events for real-time updates
    this.socketSubscription = new Subscription();
    
    // Handle new incoming messages
    this.socketSubscription.add(this.socketService.onMessage.subscribe((msg: any) => {
      // If we are talking to the sender, add the message to history
      if (this.selectedUser && (msg.senderId === this.selectedUser.id)) {
        this.messages = [...this.messages, { ...msg, timestamp: new Date(msg.timestamp), isOwn: msg.senderId === this.currentUser.id }];
        this.shouldScrollToBottom = true;
        // Auto-read if conversation is already open
        this.messagingService.markAsRead(this.selectedUser.id).subscribe();
      }
      // Reload contact list summaries
      this.loadContacts();
    }));

    // Handle message updates (edit/delete)
    this.socketSubscription.add(this.socketService.onMessageUpdate.subscribe((updatedMsg: any) => {
      const idx = this.messages.findIndex(m => m.id === updatedMsg.id);
      if (idx !== -1) {
        this.messages[idx] = { ...updatedMsg, timestamp: new Date(updatedMsg.timestamp), isOwn: updatedMsg.senderId === this.currentUser.id };
      }
      this.loadContacts();
    }));

    // Handle read status updates from the other person
    this.socketSubscription.add(this.socketService.onReadUpdate.subscribe((readerId: string) => {
      if (this.selectedUser?.id === readerId) {
        this.messages.forEach(m => {
          if (m.senderId === this.currentUser.id) m.status = 'READ';
        });
      }
      this.loadContacts();
    }));

    // 2. Reduce the background "heartbeat" to once per minute (for DB lastActive)
    this.pollSubscription = interval(60000).subscribe(() => {
      if (this.currentUser) {
        this.messagingService.ping().subscribe();
      }
      // Contacts and online status will naturally refresh, but we do one full reload per min as backup
      this.loadContacts();
    });
  }

  ngOnDestroy(): void {
    this.pollSubscription?.unsubscribe();
    this.socketSubscription?.unsubscribe();
  }

  loadContacts(): void {
    this.messagingService.getUsers().subscribe(users => {
      this.messagingService.getConversationSummaries().subscribe(summaries => {
        const summaryMap = new Map(summaries.map(s => [s.contactId, s]));

        this.contacts = users
          .filter(u => u.id !== this.currentUser.id)
          .map(u => {
            const summary: any = summaryMap.get(u.id);
            return {
              ...u,
              initials: `${u.firstName?.charAt(0) || ''}${u.lastName?.charAt(0) || ''}`.toUpperCase() || 'U',
              lastMessage: summary?.lastMessage || '',
              lastTime: summary?.lastTime ? new Date(summary.lastTime) : null,
              unreadCount: summary?.unreadCount || 0,
              // A user is considered online if lastActive is within 2 minutes (120000ms)
              online: u.lastActive ? (new Date().getTime() - new Date(u.lastActive).getTime() < 120000) : false
            };
          })
          .sort((a, b) => {
            // Sort by most recent message first, then alphabetical if no message
            if (a.lastTime && b.lastTime) return b.lastTime.getTime() - a.lastTime.getTime();
            if (a.lastTime) return -1;
            if (b.lastTime) return 1;
            return (a.firstName + a.lastName).localeCompare(b.firstName + b.lastName);
          });

        // Sync the online state to the currently active chat header
        if (this.selectedUser) {
          const updatedSelected = this.contacts.find(c => c.id === this.selectedUser!.id);
          if (updatedSelected) {
            this.selectedUser.online = updatedSelected.online;
          }
        }
      });
    });
  }

  refreshData(): void {
    if (this.selectedUser) {
      this.loadHistory(this.selectedUser.id, false);
    }
  }

  loadHistory(otherUserId: string, scroll: boolean = true): void {
    this.messagingService.getHistory(otherUserId).subscribe(msgs => {
      const newMsgs = msgs.map(m => ({
        ...m,
        timestamp: new Date(m.timestamp),
        isOwn: m.senderId === this.currentUser.id
      }));
      
      // Better deduplication: Update if counts differ or if any ID is missing from local
      const currentIds = new Set(this.messages.map(m => m.id));
      const hasNew = newMsgs.some(m => !currentIds.has(m.id));

      if (hasNew || newMsgs.length !== this.messages.length) {
        this.messages = newMsgs;
        if (scroll) this.shouldScrollToBottom = true;
      }
    });
  }

  get filteredContacts(): UserContact[] {
    if (!this.searchQuery || !this.searchQuery.trim()) return this.contacts;
    const q = this.searchQuery.toLowerCase().trim();
    return this.contacts.filter(c => 
      c.firstName?.toLowerCase().includes(q) || 
      c.lastName?.toLowerCase().includes(q) || 
      c.role?.toLowerCase().includes(q) ||
      `${c.firstName} ${c.lastName}`.toLowerCase().includes(q)
    );
  }

  selectUser(user: UserContact): void {
    if (this.selectedUser?.id === user.id) return;
    this.selectedUser = user;
    user.unreadCount = 0; // Immediate UI reset
    this.messages = [];
    this.newMessage = ''; // Reset input field when switching contact
    this.loadHistory(user.id);
    this.messagingService.markAsRead(user.id).subscribe();
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.selectedUser) return;

    const textPayload = this.newMessage.trim();
    this.newMessage = ''; // Clear immediately for better UX
    
    const msg: ChatMessage = {
      senderId: this.currentUser.id,
      receiverId: this.selectedUser.id,
      content: textPayload,
      timestamp: new Date(),
      status: 'SENT'
    };

    this.messagingService.sendMessage(msg).subscribe(savedMsg => {
      // Refresh history immediately to get the official server state (with ID)
      // This avoids duplicates because loadHistory now deduplicates by ID
      this.loadHistory(this.selectedUser!.id, true);
    });
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      if (this.editingMessageId) {
        this.saveEdit();
      } else {
        this.sendMessage();
      }
    } else if (event.key === 'Escape' && this.editingMessageId) {
      this.cancelEdit();
    }
  }

  startEdit(msg: ChatMessage): void {
    if (!this.canEdit(msg)) return;
    this.editingMessageId = msg.id || null;
    this.editContent = msg.content;
  }

  canEdit(msg: ChatMessage): boolean {
    if (!msg.isOwn || msg.attachmentId) return false;
    const now = new Date();
    const sentAt = new Date(msg.timestamp);
    const diffInMinutes = (now.getTime() - sentAt.getTime()) / (1000 * 60);
    return diffInMinutes <= 3;
  }

  cancelEdit(): void {
    this.editingMessageId = null;
    this.editContent = '';
  }

  saveEdit(): void {
    if (!this.editingMessageId || !this.editContent.trim()) return;
    
    this.messagingService.editMessage(this.editingMessageId, this.editContent.trim()).subscribe(() => {
      const msg = this.messages.find(m => m.id === this.editingMessageId);
      if (msg) {
        msg.content = this.editContent.trim();
        msg.edited = true;
      }
      this.cancelEdit();
    });
  }

  isMessageDeleted(msg: ChatMessage): boolean {
    if (msg.deletedForEveryone) return true;
    return !!(msg.isOwn ? msg.deletedForSender : msg.deletedForReceiver);
  }

  deleteMessage(msg: ChatMessage): void {
    if (!msg.id) return;

    if (msg.isOwn) {
      // Sender: show both options
      this.pendingDeleteMsg = msg;
      this.isOwnMessageModal = true;
      this.showDeleteModal = true;
    } else {
      // Receiver: only "delete for me" option
      this.pendingDeleteMsg = msg;
      this.isOwnMessageModal = false;
      this.showDeleteModal = true;
    }
  }

  confirmDeleteForMe(): void {
    const msg = this.pendingDeleteMsg;
    if (!msg?.id) return;
    this.messagingService.deleteMessage(msg.id, false).subscribe(() => {
      if (msg.isOwn) {
        msg.deletedForSender = true;  // Sender deletes it from their own view
      } else {
        msg.deletedForReceiver = true; // Receiver deletes it from their view only
      }
    });
    this.closeDeleteModal();
  }

  confirmDeleteForEveryone(): void {
    const msg = this.pendingDeleteMsg;
    if (!msg?.id) return;
    this.messagingService.deleteMessage(msg.id, true).subscribe(() => {
      msg.deletedForSender = true;
      msg.deletedForReceiver = true;
      msg.deletedForEveryone = true;
    });
    this.closeDeleteModal();
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.pendingDeleteMsg = null;
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0 || !this.selectedUser) return;

    const receiverId = this.selectedUser.id;
    const files = Array.from(input.files);

    // Upload and send each file one by one
    const sendNext = (index: number) => {
      if (index >= files.length) {
        // All files sent — reload history once at the end
        if (this.selectedUser?.id === receiverId) {
          this.loadHistory(receiverId, true);
        }
        return;
      }

      const file = files[index];
      this.messagingService.uploadAttachment(file).subscribe(res => {
        const msg: ChatMessage = {
          senderId: this.currentUser.id,
          receiverId: receiverId,
          content: '',
          timestamp: new Date(),
          status: 'SENT',
          attachmentId: res.attachmentId,
          fileName: res.fileName,
          fileType: res.fileType
        };

        this.messagingService.sendMessage(msg).subscribe(() => {
          sendNext(index + 1); // Send next file after this one is confirmed
        });
      });
    };

    sendNext(0);
    input.value = ''; // Reset input so same files can be re-selected
  }

  downloadFile(msg: ChatMessage): void {
    if (!msg.attachmentId) return;
    const url = this.messagingService.getAttachmentUrl(msg.attachmentId);
    window.open(url, '_blank');
  }

  formatTime(date: Date): string {
    if (!date) return '';
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    if (diff < 7 * 86400000) return date.toLocaleDateString([], { weekday: 'short' });
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }

  formatMsgTime(date: Date): string {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  isSameDay(d1: Date, d2: Date): boolean {
    return d1.toDateString() === d2.toDateString();
  }

  getDayLabel(date: Date): string {
    const now = new Date();
    if (this.isSameDay(date, now)) return 'Today';
    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    if (this.isSameDay(date, yesterday)) return 'Yesterday';
    return date.toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric' });
  }

  shouldShowDayDivider(messages: ChatMessage[], index: number): boolean {
    if (index === 0) return true;
    return !this.isSameDay(messages[index - 1].timestamp, messages[index].timestamp);
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  private scrollToBottom(): void {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch { }
  }
}
