/**
 * 🔒 SecuritySceneWrapper - Security Dashboard
 * 
 * Wraps existing SecurityScene component
 * Monitors: Failed logins, Suspicious activity, Blocked IPs, Rate limits
 */

import React from 'react';
import { SecurityScene } from './SecurityScene';

export const SecuritySceneWrapper = ({
  height = 1000,
  timeRange = { from: 'now-24h', to: 'now' },
}) => {
  return (
    <SecurityScene 
      height={height} 
      timeRange={timeRange} 
    />
  );
};

export default SecuritySceneWrapper;
