import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  Alert,
  Chip,
  Divider,
  CircularProgress,
  List,
  ListItem,
  ListItemText,
} from '@mui/material';
import { CheckCircle, Cancel, Add, Remove } from '@mui/icons-material';

interface Proposal {
  proposalId: number;
  entity: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  author: string;
  description: string;
  createdAt: string;
}

interface WorkflowDiff {
  addedNodes: Array<{ id: string; type: string }>;
  removedNodes: Array<{ id: string; type: string }>;
  addedEdges: Array<{ id: string; source: string; target: string }>;
  removedEdges: Array<{ id: string; source: string; target: string }>;
}

interface ProposalReviewDialogProps {
  open: boolean;
  onClose: () => void;
  proposal: Proposal | null;
  onReviewed?: () => void;
}

/**
 * W3: Dialog for reviewing and approving/rejecting workflow proposals
 */
export function ProposalReviewDialog({
  open,
  onClose,
  proposal,
  onReviewed,
}: ProposalReviewDialogProps) {
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [diff, setDiff] = useState<WorkflowDiff | null>(null);
  const [loadingDiff, setLoadingDiff] = useState(false);

  useEffect(() => {
    if (proposal && open) {
      loadDiff();
    }
  }, [proposal, open]);

  const loadDiff = async () => {
    if (!proposal) return;

    setLoadingDiff(true);
    try {
      const response = await fetch(`/api/admin/workflows/proposals/${proposal.proposalId}/diff`);
      if (response.ok) {
        const data = await response.json();
        setDiff(data);
      }
    } catch (err) {
      console.error('Failed to load diff:', err);
    } finally {
      setLoadingDiff(false);
    }
  };

  const handleApprove = async () => {
    if (!proposal) return;

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/admin/workflows/proposals/${proposal.proposalId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          approver: 'admin', // TODO: use actual user
          comment: comment || 'Approved',
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      setComment('');
      onReviewed?.();
      onClose();
    } catch (err) {
      console.error('Failed to approve proposal:', err);
      setError('Nepodařilo se schválit návrh');
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    if (!proposal) return;

    if (!comment.trim()) {
      setError('Při zamítnutí je komentář povinný');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/admin/workflows/proposals/${proposal.proposalId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          reviewer: 'admin', // TODO: use actual user
          comment,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      setComment('');
      onReviewed?.();
      onClose();
    } catch (err) {
      console.error('Failed to reject proposal:', err);
      setError('Nepodařilo se zamítnout návrh');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
      setError(null);
      setComment('');
      onClose();
    }
  };

  if (!proposal) {
    return null;
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>Zkontrolovat návrh #{proposal.proposalId}</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {error && (
            <Alert severity="error" onClose={() => setError(null)}>
              {error}
            </Alert>
          )}

          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Informace o návrhu
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1 }}>
              <Chip label={`Entity: ${proposal.entity}`} size="small" />
              <Chip label={`Autor: ${proposal.author}`} size="small" />
              <Chip label={new Date(proposal.createdAt).toLocaleDateString('cs-CZ')} size="small" />
            </Box>
            <Typography variant="body2" color="text.secondary">
              {proposal.description}
            </Typography>
          </Box>

          <Divider />

          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Změny v workflow
            </Typography>
            {loadingDiff ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                <CircularProgress size={24} />
              </Box>
            ) : diff ? (
              <List dense>
                {diff.addedNodes.length > 0 && (
                  <>
                    <Typography variant="caption" color="success.main" sx={{ fontWeight: 'bold' }}>
                      Přidané nodes:
                    </Typography>
                    {diff.addedNodes.map((node) => (
                      <ListItem key={node.id}>
                        <Add fontSize="small" color="success" sx={{ mr: 1 }} />
                        <ListItemText
                          primary={node.id}
                          secondary={node.type}
                          primaryTypographyProps={{ variant: 'body2' }}
                          secondaryTypographyProps={{ variant: 'caption' }}
                        />
                      </ListItem>
                    ))}
                  </>
                )}
                {diff.removedNodes.length > 0 && (
                  <>
                    <Typography variant="caption" color="error.main" sx={{ fontWeight: 'bold', mt: 1 }}>
                      Odebrané nodes:
                    </Typography>
                    {diff.removedNodes.map((node) => (
                      <ListItem key={node.id}>
                        <Remove fontSize="small" color="error" sx={{ mr: 1 }} />
                        <ListItemText
                          primary={node.id}
                          secondary={node.type}
                          primaryTypographyProps={{ variant: 'body2' }}
                          secondaryTypographyProps={{ variant: 'caption' }}
                        />
                      </ListItem>
                    ))}
                  </>
                )}
                {diff.addedEdges.length > 0 && (
                  <>
                    <Typography variant="caption" color="success.main" sx={{ fontWeight: 'bold', mt: 1 }}>
                      Přidané edges:
                    </Typography>
                    {diff.addedEdges.map((edge) => (
                      <ListItem key={edge.id}>
                        <Add fontSize="small" color="success" sx={{ mr: 1 }} />
                        <ListItemText
                          primary={`${edge.source} → ${edge.target}`}
                          primaryTypographyProps={{ variant: 'body2' }}
                        />
                      </ListItem>
                    ))}
                  </>
                )}
                {diff.removedEdges.length > 0 && (
                  <>
                    <Typography variant="caption" color="error.main" sx={{ fontWeight: 'bold', mt: 1 }}>
                      Odebrané edges:
                    </Typography>
                    {diff.removedEdges.map((edge) => (
                      <ListItem key={edge.id}>
                        <Remove fontSize="small" color="error" sx={{ mr: 1 }} />
                        <ListItemText
                          primary={`${edge.source} → ${edge.target}`}
                          primaryTypographyProps={{ variant: 'body2' }}
                        />
                      </ListItem>
                    ))}
                  </>
                )}
                {diff.addedNodes.length === 0 &&
                  diff.removedNodes.length === 0 &&
                  diff.addedEdges.length === 0 &&
                  diff.removedEdges.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                      Žádné změny
                    </Typography>
                  )}
              </List>
            ) : (
              <Typography variant="body2" color="text.secondary">
                Diff není k dispozici
              </Typography>
            )}
          </Box>

          <Divider />

          <TextField
            label="Komentář"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            fullWidth
            multiline
            rows={3}
            disabled={loading}
            placeholder="Volitelný komentář k rozhodnutí..."
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Zrušit
        </Button>
        <Button
          onClick={handleReject}
          startIcon={<Cancel />}
          color="error"
          disabled={loading || !comment.trim()}
        >
          Zamítnout
        </Button>
        <Button
          onClick={handleApprove}
          startIcon={<CheckCircle />}
          variant="contained"
          color="success"
          disabled={loading}
        >
          Schválit
        </Button>
      </DialogActions>
    </Dialog>
  );
}
