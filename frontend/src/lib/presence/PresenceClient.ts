/**
 * WebSocket client for real-time presence tracking
 * 
 * Protocol:
 * - Client → Server: {"type":"SUB", "userId":"...", "tenantId":"...", "entity":"Order", "id":"123"}
 * - Server → Client: {"type":"PRESENCE", "users":["user1","user2"], "stale":false, "busyBy":null, "version":5}
 * - Client → Server: {"type":"HB"} every 30s
 * - Client → Server: {"type":"LOCK", "field":"totalAmount"}
 * - Server → Client: {"type":"LOCK_ACK", "field":"totalAmount", "success":true}
 */

export interface PresenceMessage {
  type: 'SUB' | 'UNSUB' | 'HB' | 'LOCK' | 'UNLOCK' | 'PRESENCE' | 'LOCK_ACK' | 'UNLOCK_ACK' | 'HB_ACK' | 'UNSUB_ACK' | 'ERROR';
  
  // For SUB messages
  userId?: string;
  tenantId?: string;
  entity?: string;
  id?: string;
  
  // For PRESENCE messages
  users?: string[];
  stale?: boolean;
  busyBy?: string | null;
  version?: number | null;
  
  // For LOCK/UNLOCK messages
  field?: string;
  success?: boolean;
  
  // For ERROR messages
  error?: string;
}

export interface PresenceConfig {
  url: string; // ws://localhost:8080/ws/presence
  userId: string;
  tenantId: string;
  entity: string;
  id: string;
  onPresenceUpdate?: (users: string[], stale: boolean, busyBy: string | null, version: number | null) => void;
  onLockResult?: (field: string, success: boolean) => void;
  onError?: (error: string) => void;
  onConnectionChange?: (connected: boolean, loading: boolean, reconnecting: boolean, reconnectAttempt: number) => void; // NEW
}

export class PresenceClient {
  private ws: WebSocket | null = null;
  private config: PresenceConfig;
  private heartbeatInterval: number | null = null;
  private reconnectTimeout: number | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private isIntentionallyClosed = false;

  constructor(config: PresenceConfig) {
    this.config = config;
  }

  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      console.warn('[PresenceClient] Already connected');
      return;
    }

    this.isIntentionallyClosed = false;

    try {
      this.ws = new WebSocket(this.config.url);

      this.ws.onopen = () => {
        console.log('[PresenceClient] Connected');
        this.reconnectAttempts = 0;
        this.config.onConnectionChange?.(true, false, false, 0); // connected, not loading, not reconnecting
        this.subscribe();
        this.startHeartbeat();
      };

      this.ws.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.ws.onerror = (event) => {
        console.error('[PresenceClient] WebSocket error', event);
        this.config.onError?.('WebSocket connection error');
      };

      this.ws.onclose = () => {
        console.log('[PresenceClient] Disconnected');
        this.stopHeartbeat();
        this.config.onConnectionChange?.(false, false, false, 0); // disconnected
        
        if (!this.isIntentionallyClosed) {
          this.scheduleReconnect();
        }
      };
    } catch (error) {
      console.error('[PresenceClient] Failed to connect', error);
      this.scheduleReconnect();
    }
  }

  disconnect(): void {
    this.isIntentionallyClosed = true;
    
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    this.stopHeartbeat();

    if (this.ws) {
      this.unsubscribe();
      this.ws.close();
      this.ws = null;
    }
  }

  private subscribe(): void {
    const message: PresenceMessage = {
      type: 'SUB',
      userId: this.config.userId,
      tenantId: this.config.tenantId,
      entity: this.config.entity,
      id: this.config.id,
    };
    this.send(message);
  }

  private unsubscribe(): void {
    const message: PresenceMessage = { type: 'UNSUB' };
    this.send(message);
  }

  acquireLock(field: string): void {
    const message: PresenceMessage = {
      type: 'LOCK',
      field,
    };
    this.send(message);
  }

  releaseLock(field: string): void {
    const message: PresenceMessage = {
      type: 'UNLOCK',
      field,
    };
    this.send(message);
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    
    this.heartbeatInterval = window.setInterval(() => {
      const message: PresenceMessage = { type: 'HB' };
      this.send(message);
    }, 30000); // 30s interval
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private scheduleReconnect(): void {
    if (this.isIntentionallyClosed) {
      return;
    }

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[PresenceClient] Max reconnect attempts reached');
      this.config.onError?.('Failed to reconnect after multiple attempts');
      this.config.onConnectionChange?.(false, false, false, this.reconnectAttempts); // failed
      return;
    }

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    console.log(`[PresenceClient] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts + 1})`);
    
    // Notify that we're reconnecting
    this.config.onConnectionChange?.(false, false, true, this.reconnectAttempts + 1); // reconnecting

    this.reconnectTimeout = window.setTimeout(() => {
      this.reconnectAttempts++;
      this.connect();
    }, delay);
  }

  private handleMessage(data: string): void {
    try {
      const message: PresenceMessage = JSON.parse(data);

      switch (message.type) {
        case 'PRESENCE':
          this.config.onPresenceUpdate?.(
            message.users || [],
            message.stale || false,
            message.busyBy || null,
            message.version || null
          );
          break;

        case 'LOCK_ACK':
        case 'UNLOCK_ACK':
          if (message.field !== undefined && message.success !== undefined) {
            this.config.onLockResult?.(message.field, message.success);
          }
          break;

        case 'ERROR':
          console.error('[PresenceClient] Server error:', message.error);
          this.config.onError?.(message.error || 'Unknown error');
          break;

        case 'HB_ACK':
        case 'UNSUB_ACK':
          // Acknowledge messages, no action needed
          break;

        default:
          console.warn('[PresenceClient] Unknown message type:', message.type);
      }
    } catch (error) {
      console.error('[PresenceClient] Failed to parse message', error);
    }
  }

  private send(message: PresenceMessage): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('[PresenceClient] WebSocket not connected, cannot send message');
    }
  }
}
