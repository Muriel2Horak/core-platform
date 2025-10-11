import React from 'react';
import { Lock, LockOpen } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';

export interface FieldLockIndicatorProps {
  locked: boolean;
  lockedBy?: string;
  onAcquireLock?: () => void;
  onReleaseLock?: () => void;
  getUserDisplayName?: (userId: string) => string;
}

/**
 * Visual indicator for field-level locks
 * 
 * Shows lock icon when field is locked
 * - Red lock: Locked by another user
 * - Green lock: Locked by you
 * - Click to acquire/release lock
 */
export function FieldLockIndicator({
  locked,
  lockedBy,
  onAcquireLock,
  onReleaseLock,
  getUserDisplayName = (userId) => userId,
}: FieldLockIndicatorProps) {
  const handleClick = () => {
    if (locked && onReleaseLock) {
      onReleaseLock();
    } else if (!locked && onAcquireLock) {
      onAcquireLock();
    }
  };

  if (!locked) {
    return (
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <button
              type="button"
              onClick={handleClick}
              className="inline-flex items-center justify-center rounded p-1 hover:bg-muted"
            >
              <LockOpen className="h-4 w-4 text-muted-foreground" />
            </button>
          </TooltipTrigger>
          <TooltipContent>
            <p>Click to lock this field</p>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    );
  }

  const isMyLock = lockedBy === undefined; // If we don't know who locked it, assume it's us

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <button
            type="button"
            onClick={isMyLock ? handleClick : undefined}
            className="inline-flex items-center justify-center rounded p-1 hover:bg-muted"
            disabled={!isMyLock}
          >
            <Lock className={`h-4 w-4 ${isMyLock ? 'text-green-600' : 'text-red-600'}`} />
          </button>
        </TooltipTrigger>
        <TooltipContent>
          <p>
            {isMyLock
              ? 'Locked by you (click to unlock)'
              : `Locked by ${lockedBy ? getUserDisplayName(lockedBy) : 'another user'}`}
          </p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}
