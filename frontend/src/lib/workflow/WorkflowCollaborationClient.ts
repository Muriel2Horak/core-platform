/**
 * 🔄 W6: WebSocket client for real-time workflow collaboration
 * 
 * Protocol:
 * - Client → Server: {"type":"JOIN", "entity":"Order", "userId":"u1", "username":"John"}
 * - Server → Client: {"type":"USER_JOINED", "entity":"Order", "userId":"u1", "username":"John", "users":[...]}
 * - Client → Server: {"type":"NODE_UPDATE", "entity":"Order", "node":{...}}
 * - Server → Client: {"type":"NODE_UPDATED", "entity":"Order", "node":{...}, "userId":"u2"}
 * - Client → Server: {"type":"CURSOR", "entity":"Order", "x":100, "y":200}
 * - Server → Client: {"type":"CURSOR_MOVED", "entity":"Order", "userId":"u2", "x":100, "y":200}
 */

import type { Node, Edge } from 'reactflow';

export interface CollaborationMessage {
  type: 'JOIN' | 'LEAVE' | 'NODE_UPDATE' | 'EDGE_UPDATE' | 'NODE_DELETE' | 'EDGE_DELETE' | 'CURSOR' | 'HB' |
        'USER_JOINED' | 'USER_LEFT' | 'NODE_UPDATED' | 'EDGE_UPDATED' | 'NODE_DELETED' | 'EDGE_DELETED' | 
        'CURSOR_MOVED' | 'HB_ACK' | 'ERROR';
  
  // For JOIN/LEAVE
  entity?: string;
  userId?: string;
  username?: string;
  
  // For USER_JOINED/USER_LEFT
  users?: { userId: string; username: string }[];
  
  // For NODE_UPDATE/NODE_UPDATED
  node?: Partial<Node>;
  
  // For EDGE_UPDATE/EDGE_UPDATED
  edge?: Partial<Edge>;
  
  // For NODE_DELETE/NODE_DELETED
  nodeId?: string;
  
  // For EDGE_DELETE/EDGE_DELETED
  edgeId?: string;
  
  // For CURSOR/CURSOR_MOVED
  x?: number;
  y?: number;
  
  // For ERROR
  message?: string;
}

export interface CollaborationConfig {
  url: string; // ws://localhost:8080/ws/workflow
  entity: string;
  userId: string;
  username: string;
  
  onUserJoined?: (userId: string, username: string, users: { userId: string; username: string }[]) => void;
  onUserLeft?: (userId: string, users: { userId: string; username: string }[]) => void;
  onNodeUpdated?: (node: Partial<Node>, userId: string) => void;
  onEdgeUpdated?: (edge: Partial<Edge>, userId: string) => void;
  onNodeDeleted?: (nodeId: string, userId: string) => void;
  onEdgeDeleted?: (edgeId: string, userId: string) => void;
  onCursorMoved?: (userId: string, username: string, x: number, y: number) => void;
  onError?: (error: string) => void;
  onConnectionChange?: (connected: boolean) => void;
}

export class WorkflowCollaborationClient {
  private ws: WebSocket | null = null;
  private config: CollaborationConfig;
  private heartbeatInterval: number | null = null;
  private reconnectTimeout: number | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private isIntentionallyClosed = false;

  constructor(config: CollaborationConfig) {
    this.config = config;
  }

  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      console.warn('[WorkflowCollaboration] Already connected');
      return;
    }

    this.isIntentionallyClosed = false;

    try {
      this.ws = new WebSocket(this.config.url);

      this.ws.onopen = () => {
        console.log('[WorkflowCollaboration] Connected');
        this.reconnectAttempts = 0;
        this.config.onConnectionChange?.(true);
        this.join();
        this.startHeartbeat();
      };

      this.ws.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.ws.onerror = (error) => {
        console.error('[WorkflowCollaboration] WebSocket error', error);
        this.config.onError?.('WebSocket connection error');
      };

      this.ws.onclose = () => {
        console.log('[WorkflowCollaboration] Disconnected');
        this.config.onConnectionChange?.(false);
        this.stopHeartbeat();

        if (!this.isIntentionallyClosed && this.reconnectAttempts < this.maxReconnectAttempts) {
          this.scheduleReconnect();
        }
      };
    } catch (error) {
      console.error('[WorkflowCollaboration] Connection failed', error);
      this.config.onError?.('Failed to connect to collaboration server');
    }
  }

  disconnect(): void {
    this.isIntentionallyClosed = true;
    this.stopHeartbeat();

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.ws) {
      this.leave();
      this.ws.close();
      this.ws = null;
    }
  }

  // ========== Outgoing Messages ==========

  private join(): void {
    this.send({
      type: 'JOIN',
      entity: this.config.entity,
      userId: this.config.userId,
      username: this.config.username
    });
  }

  private leave(): void {
    this.send({
      type: 'LEAVE',
      entity: this.config.entity
    });
  }

  sendNodeUpdate(node: Partial<Node>): void {
    this.send({
      type: 'NODE_UPDATE',
      entity: this.config.entity,
      node
    });
  }

  sendEdgeUpdate(edge: Partial<Edge>): void {
    this.send({
      type: 'EDGE_UPDATE',
      entity: this.config.entity,
      edge
    });
  }

  sendNodeDelete(nodeId: string): void {
    this.send({
      type: 'NODE_DELETE',
      entity: this.config.entity,
      nodeId
    });
  }

  sendEdgeDelete(edgeId: string): void {
    this.send({
      type: 'EDGE_DELETE',
      entity: this.config.entity,
      edgeId
    });
  }

  sendCursor(x: number, y: number): void {
    this.send({
      type: 'CURSOR',
      entity: this.config.entity,
      x,
      y
    });
  }

  // ========== Message Handling ==========

  private handleMessage(data: string): void {
    try {
      const message: CollaborationMessage = JSON.parse(data);

      switch (message.type) {
        case 'USER_JOINED':
          if (message.userId && message.username && message.users) {
            this.config.onUserJoined?.(message.userId, message.username, message.users);
          }
          break;

        case 'USER_LEFT':
          if (message.userId && message.users) {
            this.config.onUserLeft?.(message.userId, message.users);
          }
          break;

        case 'NODE_UPDATED':
          if (message.node && message.userId) {
            this.config.onNodeUpdated?.(message.node, message.userId);
          }
          break;

        case 'EDGE_UPDATED':
          if (message.edge && message.userId) {
            this.config.onEdgeUpdated?.(message.edge, message.userId);
          }
          break;

        case 'NODE_DELETED':
          if (message.nodeId && message.userId) {
            this.config.onNodeDeleted?.(message.nodeId, message.userId);
          }
          break;

        case 'EDGE_DELETED':
          if (message.edgeId && message.userId) {
            this.config.onEdgeDeleted?.(message.edgeId, message.userId);
          }
          break;

        case 'CURSOR_MOVED':
          if (message.userId && message.username && message.x !== undefined && message.y !== undefined) {
            this.config.onCursorMoved?.(message.userId, message.username, message.x, message.y);
          }
          break;

        case 'HB_ACK':
          // Heartbeat acknowledged
          break;

        case 'ERROR':
          this.config.onError?.(message.message || 'Unknown error');
          break;

        default:
          console.warn('[WorkflowCollaboration] Unknown message type:', message.type);
      }
    } catch (error) {
      console.error('[WorkflowCollaboration] Failed to parse message', error);
    }
  }

  private send(message: CollaborationMessage): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('[WorkflowCollaboration] Cannot send message: not connected');
    }
  }

  // ========== Heartbeat & Reconnect ==========

  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.heartbeatInterval = window.setInterval(() => {
      this.send({ type: 'HB' });
    }, 30000); // 30s
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private scheduleReconnect(): void {
    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 30000); // Exponential backoff, max 30s

    console.log(`[WorkflowCollaboration] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

    this.reconnectTimeout = window.setTimeout(() => {
      this.connect();
    }, delay);
  }
}
