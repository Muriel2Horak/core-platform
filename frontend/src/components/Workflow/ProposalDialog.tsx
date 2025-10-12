import { useState } from 'react';
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
} from '@mui/material';
import { Node, Edge } from 'reactflow';

interface ProposalDialogProps {
  open: boolean;
  onClose: () => void;
  entity: string;
  nodes: Node[];
  edges: Edge[];
  onProposalCreated?: (proposalId: number) => void;
}

/**
 * W3: Dialog for creating workflow proposal
 */
export function ProposalDialog({
  open,
  onClose,
  entity,
  nodes,
  edges,
  onProposalCreated,
}: ProposalDialogProps) {
  const [description, setDescription] = useState('');
  const [author, setAuthor] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!description.trim()) {
      setError('Popis je povinný');
      return;
    }

    if (!author.trim()) {
      setError('Autor je povinný');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/admin/workflows/${entity}/proposals`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          draftData: {
            nodes: nodes.map(n => ({
              id: n.id,
              type: n.type,
              position: n.position,
              data: n.data,
            })),
            edges: edges.map(e => ({
              id: e.id,
              source: e.source,
              target: e.target,
              sourceHandle: e.sourceHandle,
              targetHandle: e.targetHandle,
              label: e.label,
            })),
          },
          author,
          description,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      
      // Reset form
      setDescription('');
      setAuthor('');
      
      // Notify parent
      onProposalCreated?.(result.proposalId);
      
      onClose();
    } catch (err) {
      console.error('Failed to create proposal:', err);
      setError('Nepodařilo se vytvořit návrh. Zkuste to znovu.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
      setError(null);
      onClose();
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Vytvořit návrh změny workflow</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {error && (
            <Alert severity="error" onClose={() => setError(null)}>
              {error}
            </Alert>
          )}

          <Typography variant="body2" color="text.secondary">
            Entity: <strong>{entity}</strong>
          </Typography>

          <Typography variant="body2" color="text.secondary">
            Nodes: <strong>{nodes.length}</strong> | Edges: <strong>{edges.length}</strong>
          </Typography>

          <TextField
            label="Autor"
            value={author}
            onChange={(e) => setAuthor(e.target.value)}
            fullWidth
            required
            disabled={loading}
            placeholder="Vaše jméno"
          />

          <TextField
            label="Popis změny"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            fullWidth
            required
            multiline
            rows={4}
            disabled={loading}
            placeholder="Popište, co jste změnili a proč..."
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Zrušit
        </Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={loading || !description.trim() || !author.trim()}
        >
          {loading ? 'Vytvářím...' : 'Vytvořit návrh'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
