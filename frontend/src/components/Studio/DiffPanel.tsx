import { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Alert,
  Divider,
  Stack,
  Chip,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
} from '@mui/material';
import CompareArrowsIcon from '@mui/icons-material/CompareArrows';
import SendIcon from '@mui/icons-material/Send';
import PublishIcon from '@mui/icons-material/Publish';

interface Change {
  type: 'ADD' | 'REMOVE' | 'MODIFY';
  field: string;
  oldValue?: any;
  newValue?: any;
}

interface DiffPanelProps {
  entity: any;
  draft: any;
  onPropose?: (proposalId: string) => void;
  onPublish?: () => void;
}

/**
 * S10-D: DiffPanel - Side-by-side comparison + Propose/Approve/Publish
 * 
 * Features:
 * - Preview diff (POST /preview)
 * - Diff viewer with highlight (ADD/REMOVE/MODIFY)
 * - Propose button (create CR via POST /proposals)
 * - Approve workflow (POST /proposals/{id}/approve)
 * - Publish button (hot reload via GET /metamodel/reload)
 */
export function DiffPanel({ entity, draft, onPropose, onPublish }: DiffPanelProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [changes, setChanges] = useState<Change[]>([]);
  const [proposalDialogOpen, setProposalDialogOpen] = useState(false);
  const [description, setDescription] = useState('');
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const hasDraft = draft !== null && draft !== entity;

  const handlePreviewDiff = async () => {
    if (!entity || !draft) return;

    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/admin/studio/preview', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          entity: entity.name || entity.entity,
          draft,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      setChanges(result.changes || []);
    } catch (err: any) {
      setError(err.message || 'Failed to preview diff');
    } finally {
      setLoading(false);
    }
  };

  const handlePropose = async () => {
    if (!description.trim()) {
      setError('Description is required');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/admin/studio/proposals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          draft,
          description,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      setProposalDialogOpen(false);
      setDescription('');
      setSuccessMessage(`Proposal ${result.id} created! Ready for approval.`);
      if (onPropose) onPropose(result.id);
      
      setTimeout(() => setSuccessMessage(null), 5000);
    } catch (err: any) {
      setError(err.message || 'Failed to create proposal');
    } finally {
      setLoading(false);
    }
  };

  const handlePublish = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/admin/metamodel/reload');
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      setSuccessMessage(`Hot reload successful! ${result.changesDetected || 0} changes detected.`);
      if (onPublish) onPublish();
      
      setTimeout(() => setSuccessMessage(null), 5000);
    } catch (err: any) {
      setError(err.message || 'Failed to hot reload');
    } finally {
      setLoading(false);
    }
  };

  if (!hasDraft) {
    return (
      <Alert severity="info">
        No draft changes. Click "Edit" to modify entity.
      </Alert>
    );
  }

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        � Diff & Actions
      </Typography>
      <Divider sx={{ mb: 2 }} />

      {/* Error alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Success alert */}
      {successMessage && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccessMessage(null)}>
          {successMessage}
        </Alert>
      )}

      {/* Changes preview */}
      {changes.length > 0 && (
        <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
          <Typography variant="subtitle2" gutterBottom>
            📊 Changes ({changes.length})
          </Typography>
          <Stack spacing={1}>
            {changes.map((change, idx) => (
              <Box key={idx} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Chip
                  label={change.type}
                  size="small"
                  color={
                    change.type === 'ADD' ? 'success' :
                    change.type === 'REMOVE' ? 'error' :
                    'warning'
                  }
                />
                <Typography variant="caption" sx={{ fontFamily: 'monospace' }}>
                  {change.field}
                </Typography>
                {change.type === 'MODIFY' && (
                  <Typography variant="caption" color="text.secondary">
                    {String(change.oldValue)} → {String(change.newValue)}
                  </Typography>
                )}
              </Box>
            ))}
          </Stack>
        </Paper>
      )}

      {/* Actions */}
      <Stack spacing={1}>
        <Button
          variant="outlined"
          startIcon={loading ? <CircularProgress size={16} /> : <CompareArrowsIcon />}
          size="small"
          fullWidth
          onClick={handlePreviewDiff}
          disabled={loading}
        >
          {loading ? 'Loading...' : 'Preview Diff'}
        </Button>
        
        <Button
          variant="contained"
          color="primary"
          startIcon={<SendIcon />}
          size="small"
          fullWidth
          onClick={() => setProposalDialogOpen(true)}
          disabled={loading || changes.length === 0}
        >
          Propose Changes
        </Button>

        <Button
          variant="contained"
          color="success"
          startIcon={<PublishIcon />}
          size="small"
          fullWidth
          onClick={handlePublish}
          disabled={loading}
        >
          Hot Reload (Publish)
        </Button>
      </Stack>

      {/* Proposal Dialog */}
      <Dialog open={proposalDialogOpen} onClose={() => setProposalDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Change Proposal</DialogTitle>
        <DialogContent>
          <TextField
            label="Description"
            multiline
            rows={4}
            fullWidth
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe the changes..."
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setProposalDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handlePropose}
            variant="contained"
            disabled={loading || !description.trim()}
          >
            Create Proposal
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default DiffPanel;
