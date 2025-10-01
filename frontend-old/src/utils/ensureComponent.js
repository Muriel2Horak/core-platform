/**
 * 🚧 Runtime Safety Net - React error #130 prevention
 * Utility pro kontrolu komponent před renderem
 */

import React from 'react';

/**
 * Tvrdý guard pro kontrolu platnosti komponenty
 * @param {any} x - komponenta k ověření
 * @param {string} name - název komponenty pro diagnostiku
 * @throws {Error} pokud komponenta není platná
 */
export function assertIsComponent(x, name) {
  const t = typeof x;
  const isValid = !!x && (t === "function" || t === "object");
  
  if (!isValid) {
    const errorMsg = `Route element "${name}" je ${t}/undefined – zkontroluj export/import!`;
    console.error(`❌ [assertIsComponent] ${errorMsg}`, { component: x, type: t });
    throw new Error(errorMsg);
  }
  
  console.log(`✅ [assertIsComponent] Component OK: ${name}`, { type: t });
}

export const ensureComponent = (Component, routeName) => {
  console.log(`🔍 [Route Guard] Checking component for route: ${routeName}`);
  
  if (!Component) {
    const errorMsg = `Component for route "${routeName}" is undefined - check import/export!`;
    console.error(`❌ [Route Guard] ${errorMsg}`);
    throw new Error(errorMsg);
  }
  
  if (typeof Component !== 'function' && typeof Component !== 'object') {
    const errorMsg = `Invalid component type for route "${routeName}": ${typeof Component}`;
    console.error(`❌ [Route Guard] ${errorMsg}`, Component);
    throw new Error(errorMsg);
  }
  
  console.log(`✅ [Route Guard] Component OK for route: ${routeName}`, Component);
  return Component;
};

export const withRouteGuard = (Component, routeName) => {
  const GuardedComponent = (props) => {
    // Použij assertIsComponent pro tvrdší kontrolu
    assertIsComponent(Component, routeName);
    const ValidatedComponent = ensureComponent(Component, routeName);
    return <ValidatedComponent {...props} />;
  };
  
  GuardedComponent.displayName = `withRouteGuard(${routeName})`;
  return GuardedComponent;
};

export default ensureComponent;