# FWK-002: NPM Packages for UI Components

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🔥 HIGH  
**Dependencies:** -  
**Category:** CORE as Framework

---

## 📖 User Story

**As a vendor (like VendorPartner)**,  
I want to use Core Platform UI components as NPM packages,  
So that I can build custom frontends on top of CORE.

---

## 🎯 Acceptance Criteria

- ⏳ NPM packages published to npmjs.com or private registry
- ⏳ Packages: `@core-platform/ui`, `@core-platform/entity-view`, `@core-platform/workflow-designer`
- ⏳ TypeScript definitions included
- ⏳ Versioning: Semantic versioning (1.0.0, 1.1.0, 2.0.0)
- ⏳ Storybook documentation for all components

---

## 💻 Implementation

### Package Structure

```
frontend/packages/
├── @core-platform/ui/              (Material UI wrappers, theme)
├── @core-platform/entity-view/     (Dynamic CRUD views)
└── @core-platform/workflow-designer/  (State machine designer)
```

### Package: @core-platform/ui

**File:** `packages/ui/package.json`

```json
{
  "name": "@core-platform/ui",
  "version": "1.0.0",
  "description": "Core Platform UI components",
  "main": "dist/index.js",
  "types": "dist/index.d.ts",
  "scripts": {
    "build": "tsc",
    "publish": "npm publish --access public"
  },
  "peerDependencies": {
    "react": "^18.0.0",
    "@mui/material": "^5.0.0"
  },
  "devDependencies": {
    "typescript": "^5.0.0",
    "@types/react": "^18.0.0"
  }
}
```

**File:** `packages/ui/src/index.ts`

```typescript
export { EntityList } from './EntityList';
export { EntityForm } from './EntityForm';
export { WorkflowStateView } from './WorkflowStateView';
export { DynamicMenu } from './DynamicMenu';
```

### Usage Example (VendorPartner)

**File:** `vendor-partner-frontend/package.json`

```json
{
  "dependencies": {
    "@core-platform/ui": "^1.0.0",
    "@core-platform/entity-view": "^1.0.0"
  }
}
```

**File:** `vendor-partner-frontend/src/App.tsx`

```tsx
import React from 'react';
import { EntityList, EntityForm } from '@core-platform/ui';

export const CustomersPage = () => {
  return (
    <>
      <EntityList entity="ivg.Customer" />
      <EntityForm entity="ivg.Customer" />
    </>
  );
};
```

---

## 📦 Publishing Process

### 1. Build Packages

```bash
cd packages/ui
npm run build

cd ../entity-view
npm run build
```

### 2. Publish to NPM

```bash
# Login to NPM
npm login

# Publish
cd packages/ui
npm publish --access public

cd ../entity-view
npm publish --access public
```

---

## 📖 Storybook Documentation

```bash
# Start Storybook
npm run storybook

# Build static Storybook
npm run build-storybook

# Deploy to GitHub Pages
npx storybook-to-ghpages
```

---

**Last Updated:** 9. listopadu 2025
