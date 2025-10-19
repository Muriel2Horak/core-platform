/**
 * 📋 AuditSceneWrapper - Audit Dashboard
 * 
 * Wraps existing AuditScene component
 * Monitors: Audit events, User actions, System changes, Compliance
 */

import React from 'react';
import { AuditScene } from './AuditScene';

export const AuditSceneWrapper = ({
  height = 1000,
  timeRange = { from: 'now-7d', to: 'now' },
}) => {
  return (
    <AuditScene 
      height={height} 
      timeRange={timeRange} 
    />
  );
};

export default AuditSceneWrapper;
