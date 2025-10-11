import React from 'react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { Eye, Pencil, Users } from 'lucide-react';
import { PresenceState } from '@/lib/presence/usePresence';

export interface PresenceIndicatorProps {
  presence: PresenceState;
  currentUserId: string;
  getUserDisplayName?: (userId: string) => string;
  getUserAvatar?: (userId: string) => string | undefined;
}

/**
 * Presence indicator showing who is viewing/editing the entity
 * 
 * Features:
 * - Shows avatars of all active users
 * - "Stale" badge when entity is being modified (WRITE pipeline active)
 * - Busy user highlighted (who initiated the write)
 * - Version number display
 */
export function PresenceIndicator({
  presence,
  currentUserId,
  getUserDisplayName = (userId) => userId,
  getUserAvatar,
}: PresenceIndicatorProps) {
  const { users, stale, busyBy, version, connected } = presence;

  if (!connected) {
    return (
      <div className="flex items-center gap-2 text-muted-foreground text-sm">
        <div className="h-2 w-2 rounded-full bg-gray-400" />
        <span>Offline</span>
      </div>
    );
  }

  const viewingUsers = users.filter((u) => typeof u === 'string') as string[];
  const otherUsers = viewingUsers.filter((u) => u !== currentUserId);

  return (
    <div className="flex items-center gap-3">
      {/* Connection Status */}
      <div className="flex items-center gap-1.5">
        <div className="h-2 w-2 rounded-full bg-green-500 animate-pulse" />
        <span className="text-xs text-muted-foreground">Live</span>
      </div>

      {/* Stale Badge */}
      {stale && busyBy && (
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <Badge variant="destructive" className="flex items-center gap-1">
                <Pencil className="h-3 w-3" />
                Editing
              </Badge>
            </TooltipTrigger>
            <TooltipContent>
              <p>{getUserDisplayName(busyBy)} is modifying this record</p>
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>
      )}

      {/* User Avatars */}
      {viewingUsers.length > 0 && (
        <div className="flex items-center gap-2">
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <div className="flex items-center gap-1">
                  <Users className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm font-medium">{viewingUsers.length}</span>
                </div>
              </TooltipTrigger>
              <TooltipContent>
                <div className="space-y-1">
                  {viewingUsers.map((userId) => (
                    <div key={userId} className="flex items-center gap-2">
                      <Eye className="h-3 w-3" />
                      <span>{getUserDisplayName(userId)}</span>
                      {userId === currentUserId && ' (you)'}
                      {userId === busyBy && ' (editing)'}
                    </div>
                  ))}
                </div>
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>

          {/* Avatar Stack */}
          <div className="flex -space-x-2">
            {otherUsers.slice(0, 3).map((userId) => (
              <TooltipProvider key={userId}>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Avatar className="h-7 w-7 border-2 border-background">
                      <AvatarImage src={getUserAvatar?.(userId)} alt={getUserDisplayName(userId)} />
                      <AvatarFallback className="text-xs">
                        {getUserDisplayName(userId)
                          .split(' ')
                          .map((n) => n[0])
                          .join('')
                          .toUpperCase()
                          .slice(0, 2)}
                      </AvatarFallback>
                    </Avatar>
                  </TooltipTrigger>
                  <TooltipContent>
                    <p>{getUserDisplayName(userId)}</p>
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            ))}
            {otherUsers.length > 3 && (
              <div className="flex h-7 w-7 items-center justify-center rounded-full border-2 border-background bg-muted text-xs font-medium">
                +{otherUsers.length - 3}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Version Badge */}
      {version !== null && (
        <Badge variant="outline" className="text-xs">
          v{version}
        </Badge>
      )}
    </div>
  );
}
