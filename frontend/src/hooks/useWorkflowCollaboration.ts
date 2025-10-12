/**
 * 🔄 W6: React hook for real-time workflow collaboration
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { WorkflowCollaborationClient, type CollaborationConfig } from '../lib/workflow/WorkflowCollaborationClient';
import type { Node, Edge } from 'reactflow';

interface CollaborationUser {
  userId: string;
  username: string;
}

interface CollaborationCursor {
  userId: string;
  username: string;
  x: number;
  y: number;
}

interface UseWorkflowCollaborationProps {
  entity: string;
  userId: string;
  username: string;
  enabled?: boolean;
  
  onNodeUpdated?: (node: Partial<Node>, userId: string) => void;
  onEdgeUpdated?: (edge: Partial<Edge>, userId: string) => void;
  onNodeDeleted?: (nodeId: string, userId: string) => void;
  onEdgeDeleted?: (edgeId: string, userId: string) => void;
}

export function useWorkflowCollaboration({
  entity,
  userId,
  username,
  enabled = true,
  onNodeUpdated,
  onEdgeUpdated,
  onNodeDeleted,
  onEdgeDeleted
}: UseWorkflowCollaborationProps) {
  const clientRef = useRef<WorkflowCollaborationClient | null>(null);
  const [connected, setConnected] = useState(false);
  const [users, setUsers] = useState<CollaborationUser[]>([]);
  const [cursors, setCursors] = useState<Map<string, CollaborationCursor>>(new Map());
  const [error, setError] = useState<string | null>(null);

  // Initialize client
  useEffect(() => {
    if (!enabled) return;

    const wsUrl = (import.meta as any).env?.VITE_WS_URL || 'ws://localhost:8080';
    
    const config: CollaborationConfig = {
      url: `${wsUrl}/ws/workflow`,
      entity,
      userId,
      username,
      
      onUserJoined: (_joinedUserId: string, joinedUsername: string, allUsers: CollaborationUser[]) => {
        console.log(`[Collaboration] User joined: ${joinedUsername}`);
        setUsers(allUsers);
      },
      
      onUserLeft: (leftUserId: string, remainingUsers: CollaborationUser[]) => {
        console.log(`[Collaboration] User left: ${leftUserId}`);
        setUsers(remainingUsers);
        setCursors(prev => {
          const next = new Map(prev);
          next.delete(leftUserId);
          return next;
        });
      },
      
      onNodeUpdated: (node: Partial<Node>, fromUserId: string) => {
        console.log(`[Collaboration] Node updated by ${fromUserId}:`, node);
        onNodeUpdated?.(node, fromUserId);
      },
      
      onEdgeUpdated: (edge: Partial<Edge>, fromUserId: string) => {
        console.log(`[Collaboration] Edge updated by ${fromUserId}:`, edge);
        onEdgeUpdated?.(edge, fromUserId);
      },
      
      onNodeDeleted: (nodeId: string, fromUserId: string) => {
        console.log(`[Collaboration] Node deleted by ${fromUserId}: ${nodeId}`);
        onNodeDeleted?.(nodeId, fromUserId);
      },
      
      onEdgeDeleted: (edgeId: string, fromUserId: string) => {
        console.log(`[Collaboration] Edge deleted by ${fromUserId}: ${edgeId}`);
        onEdgeDeleted?.(edgeId, fromUserId);
      },
      
      onCursorMoved: (cursorUserId: string, cursorUsername: string, x: number, y: number) => {
        setCursors(prev => {
          const next = new Map(prev);
          next.set(cursorUserId, { userId: cursorUserId, username: cursorUsername, x, y });
          return next;
        });
      },
      
      onError: (err: string) => {
        console.error('[Collaboration] Error:', err);
        setError(err);
      },
      
      onConnectionChange: (isConnected: boolean) => {
        setConnected(isConnected);
        if (!isConnected) {
          setUsers([]);
          setCursors(new Map());
        }
      }
    };

    const client = new WorkflowCollaborationClient(config);
    clientRef.current = client;
    client.connect();

    return () => {
      client.disconnect();
      clientRef.current = null;
    };
  }, [entity, userId, username, enabled, onNodeUpdated, onEdgeUpdated, onNodeDeleted, onEdgeDeleted]);

  // Send node update
  const sendNodeUpdate = useCallback((node: Partial<Node>) => {
    clientRef.current?.sendNodeUpdate(node);
  }, []);

  // Send edge update
  const sendEdgeUpdate = useCallback((edge: Partial<Edge>) => {
    clientRef.current?.sendEdgeUpdate(edge);
  }, []);

  // Send node delete
  const sendNodeDelete = useCallback((nodeId: string) => {
    clientRef.current?.sendNodeDelete(nodeId);
  }, []);

  // Send edge delete
  const sendEdgeDelete = useCallback((edgeId: string) => {
    clientRef.current?.sendEdgeDelete(edgeId);
  }, []);

  // Send cursor position
  const sendCursor = useCallback((x: number, y: number) => {
    clientRef.current?.sendCursor(x, y);
  }, []);

  return {
    connected,
    users,
    cursors: Array.from(cursors.values()),
    error,
    sendNodeUpdate,
    sendEdgeUpdate,
    sendNodeDelete,
    sendEdgeDelete,
    sendCursor
  };
}
