import { Injectable } from '@angular/core';
import { Client, IMessage, Message, Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { AuthService } from '../auth.service';

@Injectable({
  providedIn: 'root'
})
export class SocketService {
  private stompClient: Client | null = null;
  
  private messageSubject = new Subject<any>();
  private alertSubject = new Subject<void>();
  private notificationSubject = new Subject<void>();
  private unreadCountSubject = new Subject<number>();
  private messageUpdateSubject = new Subject<any>();
  private readUpdateSubject = new Subject<string>();
  private userStatusSubject = new Subject<{userId: string, online: boolean}>();
  
  private connectionStatus = new BehaviorSubject<boolean>(false);

  private currentUserId: string | null = null;

  constructor(private authService: AuthService) {
    this.init();
  }

  private init() {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.connect(user.id);
    }

    // React to login/logout
    this.authService.user$.subscribe(user => {
      if (user) {
        this.connect(user.id);
      } else {
        this.disconnect();
      }
    });
  }


  private connect(userId: string) {
    if (this.stompClient && this.stompClient.connected) {
      if (this.currentUserId === userId) {
        return; // Already connected for this user
      } else {
        this.disconnect(); // Disconnect old user
      }
    }
    
    this.currentUserId = userId;

    this.stompClient = Stomp.over(() => new SockJS('/ws'));

    this.stompClient.onConnect = (frame: any) => {
      this.connectionStatus.next(true);
      console.log('Connected to WebSocket');

      // Subscribe to personal messages
      this.stompClient!.subscribe(`/topic/messages/${userId}`, (msg: Message) => {
        this.messageSubject.next(JSON.parse(msg.body));
      });

      // Subscribe to unread count updates
      this.stompClient!.subscribe(`/topic/unread-count/${userId}`, (msg: Message) => {
        const body = JSON.parse(msg.body);
        this.unreadCountSubject.next(body.count);
      });

      // Subscribe to message updates (edits/deletes/tombstones)
      this.stompClient!.subscribe(`/topic/message-updates/${userId}`, (msg: Message) => {
        this.messageUpdateSubject.next(JSON.parse(msg.body));
      });

      // Subscribe to read status updates from others
      this.stompClient!.subscribe(`/topic/read-updates/${userId}`, (msg: Message) => {
        const body = JSON.parse(msg.body);
        this.readUpdateSubject.next(body.readerId);
      });

      // Subscribe to global user online/offline status
      this.stompClient!.subscribe(`/topic/user-status`, (msg: Message) => {
        this.userStatusSubject.next(JSON.parse(msg.body));
      });

      // Unified Alerts and Notifications
      this.stompClient!.subscribe('/topic/alerts', (msg: Message) => {
        this.alertSubject.next();
      });
      this.stompClient!.subscribe('/topic/notifications', (msg: Message) => {
        this.notificationSubject.next();
      });
    };

    this.stompClient.onStompError = (frame: any) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    this.stompClient.onWebSocketClose = () => {
      this.connectionStatus.next(false);
    };

    this.stompClient.activate();
  }

  private disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.connectionStatus.next(false);
    }
  }

  get onMessage(): Observable<any> {
    return this.messageSubject.asObservable();
  }

  get onAlertUpdate(): Observable<void> {
    return this.alertSubject.asObservable();
  }

  get onNotificationUpdate(): Observable<void> {
    return this.notificationSubject.asObservable();
  }

  get onUnreadCount(): Observable<number> {
    return this.unreadCountSubject.asObservable();
  }

  get onMessageUpdate(): Observable<any> {
    return this.messageUpdateSubject.asObservable();
  }

  get onReadUpdate(): Observable<string> {
    return this.readUpdateSubject.asObservable();
  }

  get onUserStatus(): Observable<{userId: string, online: boolean}> {
    return this.userStatusSubject.asObservable();
  }

  get isConnected(): Observable<boolean> {
    return this.connectionStatus.asObservable();
  }
}
