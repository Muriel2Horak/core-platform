---
id: DMS-001-T5
story: DMS-001
title: "DocumentVersionHistory Frontend Component"
status: todo
assignee: ""
estimate: "4 hours"
created: 2025-11-08
updated: 2025-11-08
---

# DMS-001-T5: DocumentVersionHistory Frontend Component

> **Parent Story:** [DMS-001: Document Versioning](../README.md)  
> **Status:** todo | **Estimate:** 4 hours

## 🎯 Subtask Goal

Create React component `DocumentVersionHistory.tsx` using MUI Timeline to display version history with upload, download, and rollback actions.

## ✅ Acceptance Criteria

- [ ] Component displays Timeline with all versions (newest first)
- [ ] Shows: version number, created_by, created_at, file size, changeComment
- [ ] "Current" badge on latest version
- [ ] "Signed" badge for signed versions (green chip with VerifiedUser icon)
- [ ] Download button for each version
- [ ] Rollback button (disabled for current version)
- [ ] Upload New Version dialog (file picker + changeComment textarea)
- [ ] Rollback confirmation dialog
- [ ] Responsive layout (works on mobile)

## 📂 Files to Create

- `frontend/src/components/DocumentVersionHistory.tsx`

## 🔧 Implementation

**File:** `frontend/src/components/DocumentVersionHistory.tsx`

```typescript
import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Timeline, TimelineItem, TimelineSeparator, TimelineConnector,
  TimelineContent, TimelineDot, TimelineOppositeContent
} from '@mui/lab';
import {
  Typography, Paper, Button, Chip, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, IconButton, Tooltip
} from '@mui/material';
import { Download, History, VerifiedUser, Comment, Restore } from '@mui/icons-material';
import { format } from 'date-fns';
import axios from 'axios';

interface DocumentVersion {
  id: string;
  documentId: string;
  versionNumber: number;
  sizeBytes: number;
  mimeType: string;
  createdBy: string;
  createdAt: string;
  changeComment?: string;
  signedBy?: string;
  signedAt?: string;
  signatureMethod?: 'BANKID' | 'EID' | 'INTERNAL';
}

export const DocumentVersionHistory: React.FC = () => {
  const { documentId } = useParams<{ documentId: string }>();
  const [versions, setVersions] = useState<DocumentVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [rollbackDialogOpen, setRollbackDialogOpen] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState<number | null>(null);
  const [changeComment, setChangeComment] = useState('');
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  
  useEffect(() => {
    loadVersions();
  }, [documentId]);
  
  const loadVersions = async () => {
    try {
      const response = await axios.get(`/api/dms/documents/${documentId}/versions`);
      setVersions(response.data);
    } catch (error) {
      console.error('Failed to load versions:', error);
    } finally {
      setLoading(false);
    }
  };
  
  const handleUploadNewVersion = async () => {
    if (!uploadFile) return;
    
    const formData = new FormData();
    formData.append('file', uploadFile);
    if (changeComment) formData.append('changeComment', changeComment);
    
    try {
      await axios.post(`/api/dms/documents/${documentId}/versions`, formData);
      setUploadDialogOpen(false);
      setUploadFile(null);
      setChangeComment('');
      loadVersions();
    } catch (error) {
      console.error('Failed to upload version:', error);
    }
  };
  
  const handleRollback = async () => {
    if (selectedVersion === null) return;
    
    try {
      await axios.post(`/api/dms/documents/${documentId}/rollback/${selectedVersion}`);
      setRollbackDialogOpen(false);
      setSelectedVersion(null);
      loadVersions();
    } catch (error) {
      console.error('Failed to rollback:', error);
    }
  };
  
  const handleDownloadVersion = async (versionNumber: number) => {
    try {
      const response = await axios.get(`/api/dms/documents/${documentId}/versions/${versionNumber}/download`);
      window.open(response.data.downloadUrl, '_blank');
    } catch (error) {
      console.error('Failed to download version:', error);
    }
  };
  
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };
  
  if (loading) return <Typography>Loading version history...</Typography>;
  
  return (
    <Paper sx={{ p: 3 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">
          <History sx={{ mr: 1, verticalAlign: 'middle' }} />
          Version History ({versions.length} versions)
        </Typography>
        
        <Button variant="contained" onClick={() => setUploadDialogOpen(true)}>
          Upload New Version
        </Button>
      </div>
      
      <Timeline position="alternate">
        {versions.map((version, index) => (
          <TimelineItem key={version.id}>
            <TimelineOppositeContent color="text.secondary">
              {format(new Date(version.createdAt), 'dd.MM.yyyy HH:mm')}
            </TimelineOppositeContent>
            
            <TimelineSeparator>
              <TimelineDot color={index === 0 ? 'primary' : 'grey'}>
                {version.signedBy && <VerifiedUser />}
              </TimelineDot>
              {index < versions.length - 1 && <TimelineConnector />}
            </TimelineSeparator>
            
            <TimelineContent>
              <Paper elevation={3} sx={{ p: 2 }}>
                <Typography variant="h6">
                  Version {version.versionNumber}
                  {index === 0 && <Chip label="Current" color="primary" size="small" sx={{ ml: 1 }} />}
                  {version.signedBy && (
                    <Chip label={`Signed (${version.signatureMethod})`} color="success" size="small" icon={<VerifiedUser />} sx={{ ml: 1 }} />
                  )}
                </Typography>
                
                <Typography variant="body2" color="text.secondary">
                  Uploaded by: <strong>{version.createdBy}</strong>
                </Typography>
                
                <Typography variant="body2" color="text.secondary">
                  Size: {formatFileSize(version.sizeBytes)}
                </Typography>
                
                {version.changeComment && (
                  <Typography variant="body2" sx={{ mt: 1, fontStyle: 'italic' }}>
                    <Comment fontSize="small" sx={{ mr: 0.5 }} />
                    {version.changeComment}
                  </Typography>
                )}
                
                <div style={{ marginTop: 8 }}>
                  <Tooltip title="Download this version">
                    <IconButton size="small" color="primary" onClick={() => handleDownloadVersion(version.versionNumber)}>
                      <Download />
                    </IconButton>
                  </Tooltip>
                  
                  {index > 0 && (
                    <Tooltip title="Rollback to this version">
                      <IconButton size="small" color="warning" onClick={() => {
                        setSelectedVersion(version.versionNumber);
                        setRollbackDialogOpen(true);
                      }}>
                        <Restore />
                      </IconButton>
                    </Tooltip>
                  )}
                </div>
              </Paper>
            </TimelineContent>
          </TimelineItem>
        ))}
      </Timeline>
      
      {/* Upload Dialog */}
      <Dialog open={uploadDialogOpen} onClose={() => setUploadDialogOpen(false)}>
        <DialogTitle>Upload New Version</DialogTitle>
        <DialogContent>
          <input type="file" onChange={(e) => setUploadFile(e.target.files?.[0] || null)} style={{ mb: 2 }} />
          <TextField label="Change Comment (optional)" fullWidth multiline rows={3} value={changeComment} onChange={(e) => setChangeComment(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleUploadNewVersion} variant="contained" disabled={!uploadFile}>Upload</Button>
        </DialogActions>
      </Dialog>
      
      {/* Rollback Dialog */}
      <Dialog open={rollbackDialogOpen} onClose={() => setRollbackDialogOpen(false)}>
        <DialogTitle>Confirm Rollback</DialogTitle>
        <DialogContent>
          <Typography>Rollback to version {selectedVersion}?</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            This will create a new version (copy of version {selectedVersion}). All history preserved.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRollbackDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleRollback} variant="contained" color="warning">Rollback</Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};
```

## ✅ Definition of Done

- [ ] Component renders Timeline with versions
- [ ] Current version highlighted with badge
- [ ] Signed versions show green chip
- [ ] Download button works (opens presigned URL)
- [ ] Rollback button creates new version
- [ ] Upload dialog functional
- [ ] Responsive on mobile
- [ ] Committed: `feat(DMS-001-T5): Add DocumentVersionHistory component`
