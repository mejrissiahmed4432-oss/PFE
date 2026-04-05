import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';

export interface ChatMessage {
  id: string;
  senderId: string;
  senderName: string;
  senderInitials: string;
  text?: string;
  fileUrl?: string;
  fileName?: string;
  fileType?: 'image' | 'document' | 'other';
  timestamp: Date;
  isOwn: boolean;
  status: 'sent' | 'delivered' | 'read';
}

export interface Conversation {
  id: string;
  name: string;
  initials: string;
  role: string;
  lastMessage: string;
  lastTime: Date;
  unread: number;
  online: boolean;
  messages: ChatMessage[];
}

@Component({
  selector: 'app-messaging',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messaging.component.html',
  styleUrl: './messaging.component.css'
})
export class MessagingComponent implements OnInit, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  @ViewChild('fileInput') private fileInput!: ElementRef;

  currentUser: any;
  currentUserName: string = '';
  currentUserInitials: string = '';

  searchQuery: string = '';
  newMessage: string = '';
  selectedConversation: Conversation | null = null;
  shouldScrollToBottom: boolean = false;

  conversations: Conversation[] = [
    {
      id: '1',
      name: 'Ahmed Ben Salah',
      initials: 'AB',
      role: 'Warehouse Manager',
      lastMessage: 'The new shipment has arrived at dock B.',
      lastTime: new Date(Date.now() - 5 * 60000),
      unread: 3,
      online: true,
      messages: [
        {
          id: 'm1', senderId: '2', senderName: 'Ahmed Ben Salah', senderInitials: 'AB',
          text: 'Good morning! The new shipment has arrived at dock B.',
          timestamp: new Date(Date.now() - 30 * 60000), isOwn: false, status: 'read'
        },
        {
          id: 'm2', senderId: '1', senderName: 'Me', senderInitials: 'ME',
          text: 'Great, I will send someone to check the inventory.',
          timestamp: new Date(Date.now() - 25 * 60000), isOwn: true, status: 'read'
        },
        {
          id: 'm3', senderId: '2', senderName: 'Ahmed Ben Salah', senderInitials: 'AB',
          text: 'Here is the packing list for your review.',
          timestamp: new Date(Date.now() - 10 * 60000), isOwn: false, status: 'read'
        },
        {
          id: 'm4', senderId: '2', senderName: 'Ahmed Ben Salah', senderInitials: 'AB',
          text: 'The new shipment has arrived at dock B.',
          timestamp: new Date(Date.now() - 5 * 60000), isOwn: false, status: 'read'
        },
      ]
    },
    {
      id: '2',
      name: 'IT Support Team',
      initials: 'IT',
      role: 'Group · 5 members',
      lastMessage: 'Server maintenance scheduled for tonight.',
      lastTime: new Date(Date.now() - 2 * 3600000),
      unread: 0,
      online: true,
      messages: [
        {
          id: 'm5', senderId: '3', senderName: 'Sara H.', senderInitials: 'SH',
          text: 'Server maintenance is scheduled for tonight at 00:00.',
          timestamp: new Date(Date.now() - 2 * 3600000), isOwn: false, status: 'read'
        },
        {
          id: 'm6', senderId: '1', senderName: 'Me', senderInitials: 'ME',
          text: 'Acknowledged. I will notify all department heads.',
          timestamp: new Date(Date.now() - 1.5 * 3600000), isOwn: true, status: 'delivered'
        },
      ]
    },
    {
      id: '3',
      name: 'Karim Laabidi',
      initials: 'KL',
      role: 'Procurement Officer',
      lastMessage: 'Can you approve the purchase order?',
      lastTime: new Date(Date.now() - 24 * 3600000),
      unread: 1,
      online: false,
      messages: [
        {
          id: 'm7', senderId: '4', senderName: 'Karim Laabidi', senderInitials: 'KL',
          text: 'Hey, can you approve the purchase order #PO-2024-089?',
          timestamp: new Date(Date.now() - 24 * 3600000), isOwn: false, status: 'read'
        },
      ]
    },
    {
      id: '4',
      name: 'Amel Chaabane',
      initials: 'AC',
      role: 'HR Manager',
      lastMessage: 'The new onboarding checklist is ready.',
      lastTime: new Date(Date.now() - 3 * 24 * 3600000),
      unread: 0,
      online: false,
      messages: [
        {
          id: 'm8', senderId: '5', senderName: 'Amel Chaabane', senderInitials: 'AC',
          text: 'The new onboarding checklist is ready for the 3 new hires.',
          timestamp: new Date(Date.now() - 3 * 24 * 3600000), isOwn: false, status: 'read'
        },
        {
          id: 'm9', senderId: '1', senderName: 'Me', senderInitials: 'ME',
          text: 'Perfect, send it to them and copy me on the email.',
          timestamp: new Date(Date.now() - 3 * 24 * 3600000 + 5 * 60000), isOwn: true, status: 'read'
        },
      ]
    }
  ];

  ngOnInit(): void { }

  constructor(private authService: AuthService) {
    const user = this.authService.getCurrentUser();
    this.currentUser = user;
    if (user) {
      const first = user.firstName || '';
      const last = user.lastName || '';
      this.currentUserName = `${first} ${last}`.trim() || user.email || 'Me';
      this.currentUserInitials = `${first.charAt(0)}${last.charAt(0)}`.toUpperCase() || 'ME';
    }
    // Select first conversation by default
    if (this.conversations.length > 0) {
      this.selectConversation(this.conversations[0]);
    }
  }

  get filteredConversations(): Conversation[] {
    if (!this.searchQuery.trim()) return this.conversations;
    const q = this.searchQuery.toLowerCase();
    return this.conversations.filter(c =>
      c.name.toLowerCase().includes(q) || c.lastMessage.toLowerCase().includes(q)
    );
  }

  get totalUnread(): number {
    return this.conversations.reduce((sum, c) => sum + c.unread, 0);
  }

  selectConversation(conv: Conversation): void {
    this.selectedConversation = conv;
    conv.unread = 0;
    this.shouldScrollToBottom = true;
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.selectedConversation) return;

    const msg: ChatMessage = {
      id: `m_${Date.now()}`,
      senderId: '1',
      senderName: this.currentUserName,
      senderInitials: this.currentUserInitials,
      text: this.newMessage.trim(),
      timestamp: new Date(),
      isOwn: true,
      status: 'sent'
    };

    this.selectedConversation.messages.push(msg);
    this.selectedConversation.lastMessage = msg.text!;
    this.selectedConversation.lastTime = msg.timestamp;
    this.newMessage = '';
    this.shouldScrollToBottom = true;

    // Simulate delivery status update
    setTimeout(() => { msg.status = 'delivered'; }, 1000);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0 || !this.selectedConversation) return;

    const file = input.files[0];
    const isImage = file.type.startsWith('image/');
    const reader = new FileReader();

    reader.onload = () => {
      const msg: ChatMessage = {
        id: `m_${Date.now()}`,
        senderId: '1',
        senderName: this.currentUserName,
        senderInitials: this.currentUserInitials,
        fileUrl: reader.result as string,
        fileName: file.name,
        fileType: isImage ? 'image' : 'document',
        timestamp: new Date(),
        isOwn: true,
        status: 'sent'
      };
      this.selectedConversation!.messages.push(msg);
      this.selectedConversation!.lastMessage = isImage ? '📷 Photo' : `📄 ${file.name}`;
      this.selectedConversation!.lastTime = msg.timestamp;
      this.shouldScrollToBottom = true;
      setTimeout(() => { msg.status = 'delivered'; }, 1000);
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  downloadFile(msg: ChatMessage): void {
    if (!msg.fileUrl || !msg.fileName) return;
    const a = document.createElement('a');
    a.href = msg.fileUrl;
    a.download = msg.fileName;
    a.click();
  }

  formatTime(date: Date): string {
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
