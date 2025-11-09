---
id: DMS-002-T5
story: DMS-002
title: "Frontend Document List Component (1.5h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-002-T5: Frontend Document List Component (1.5h)

> **Parent Story:** [DMS-002](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Frontend Document List Component (1.5h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `frontend/src/components/EntityDocumentList.tsx`

## 🔧 Implementation Details

### Code Example 1 (tsx)

```tsx
import React, { useState, useEffect } from 'react';
import {
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Select,
  MenuItem,
  FormControl,
  InputLabel
} from '@mui/material';
import {
  Description,
  Delete,
  Download,
  Link as LinkIcon,
  DragIndicator
} from '@mui/icons-material';
import { DragDropContext, Droppable, Draggable } from 'react-beautiful-dnd';
import axios from 'axios';

interface DocumentWithLink {
  document: {
    id: string;
    filename: string;
    mimeType: string;
    sizeBytes: number;
    uploadedBy: string;
    uploadedAt: string;
  };
  link: {
    id: string;
    documentId: string;
    entityType: string;
    entityId: string;
    linkRole: 'PRIMARY' | 'ATTACHMENT' | 'CONTRACT' | 'EVIDENCE' | 'INVOICE' | 'RECEIPT';
    displayOrder: number;
    linkedBy: string;
    linkedAt: string;
  };
}

interface Props {
  entityType: string;
  entityId: string;
}

export const EntityDocumentList: React.FC<Props> = ({ entityType, entityId }) => {
  const [documents, setDocuments] = useState<DocumentWithLink[]>([]);
  const [linkDialogOpen, setLinkDialogOpen] = useState(false);
  const [selectedDocumentId, setSelectedDocumentId] = useState<string>('');
  const [selectedRole, setSelectedRole] = useState<string>('ATTACHMENT');

// ... (see parent story for complete code)
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-002. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-002-T5): Frontend Document List Component (1.5h)`
