import React, { createContext, useState, useContext, useMemo } from 'react';

const TenantContext = createContext(null);

export function TenantProvider({ children }) {
  const [tenant, setTenant] = useState(null);
  const [isTenantLoading, setIsTenantLoading] = useState(true);

  const value = useMemo(() => ({
    tenant,
    setTenant,
    isTenantLoading,
    setIsTenantLoading,
  }), [tenant, isTenantLoading]);

  return (
    <TenantContext.Provider value={value}>
      {children}
    </TenantContext.Provider>
  );
}

export function useTenant() {
  const context = useContext(TenantContext);
  if (!context) {
    throw new Error('useTenant must be used within a TenantProvider');
  }
  return context;
}
