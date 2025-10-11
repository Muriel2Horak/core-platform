/**
 * Real-time Presence Tracking System
 * 
 * Exports:
 * - PresenceClient: Low-level WebSocket client
 * - usePresence: React hook for presence tracking
 * - PresenceIndicator: UI component showing active users
 * - FieldLockIndicator: UI component for field-level locks
 * 
 * Example Usage:
 * ```tsx
 * import { usePresence, PresenceIndicator } from '@/lib/presence';
 * 
 * function OrderEditPage({ orderId, tenantId }) {
 *   const { presence, acquireLock, releaseLock } = usePresence({
 *     entity: 'Order',
 *     id: orderId,
 *     tenantId,
 *     userId: currentUser.id,
 *   });
 *   
 *   return (
 *     <div>
 *       <PresenceIndicator 
 *         presence={presence} 
 *         currentUserId={currentUser.id}
 *       />
 *       
 *       {presence.stale && (
 *         <Alert>
 *           This record is being modified by {presence.busyBy}
 *         </Alert>
 *       )}
 *     </div>
 *   );
 * }
 * ```
 */

export { PresenceClient } from './PresenceClient';
export type { PresenceMessage, PresenceConfig } from './PresenceClient';

export { usePresence } from './usePresence';
export type { PresenceState, UsePresenceOptions } from './usePresence';
