import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  List,
  ListItem,
  ListItemText,
  Chip,
  Button,
  CircularProgress,
  Alert,
  Divider,
} from '@mui/material';
import { CheckCircle, Cancel, HourglassEmpty } from '@mui/icons-material';

interface Proposal {
  proposalId: number;
  entity: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  author: string;
  description: string;
  createdAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
}

interface ProposalListPanelProps {
  entity?: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  onSelectProposal?: (proposal: Proposal) => void;
  refreshTrigger?: number;
}

/**
 * W3: Panel for listing workflow proposals
 */
export function ProposalListPanel({
  entity,
  status,
  onSelectProposal,
  refreshTrigger,
}: ProposalListPanelProps) {
  const [proposals, setProposals] = useState<Proposal[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadProposals = async () => {
    setLoading(true);
    setError(null);

    try {
      const params = new URLSearchParams();
      if (entity) params.append('entity', entity);
      if (status) params.append('status', status);

      const response = await fetch(`/api/admin/workflows/proposals?${params.toString()}`);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      setProposals(data);
    } catch (err) {
      console.error('Failed to load proposals:', err);
      setError('Nepodařilo se načíst návrhy');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProposals();
  }, [entity, status, refreshTrigger]);

  const getStatusIcon = (proposalStatus: string) => {
    switch (proposalStatus) {
      case 'APPROVED': return <CheckCircle fontSize="small" />;
      case 'REJECTED': return <Cancel fontSize="small" />;
      default: return <HourglassEmpty fontSize="small" />;
    }
  };

  const getStatusColor = (proposalStatus: string) => {
    switch (proposalStatus) {
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'error';
      default: return 'warning';
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 2 }}>
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
        <Button onClick={loadProposals} sx={{ mt: 2 }} fullWidth>
          Zkusit znovu
        </Button>
      </Box>
    );
  }

  if (proposals.length === 0) {
    return (
      <Box sx={{ p: 3, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          Žádné návrhy
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ maxHeight: '100%', overflow: 'auto' }}>
      <List>
        {proposals.map((proposal, index) => (
          <React.Fragment key={proposal.proposalId}>
            <ListItem
              sx={{
                flexDirection: 'column',
                alignItems: 'flex-start',
                cursor: onSelectProposal ? 'pointer' : 'default',
                '&:hover': onSelectProposal ? { bgcolor: 'action.hover' } : {},
              }}
              onClick={() => onSelectProposal?.(proposal)}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1, width: '100%' }}>
                <Chip
                  icon={getStatusIcon(proposal.status)}
                  label={proposal.status}
                  color={getStatusColor(proposal.status) as any}
                  size="small"
                />
                <Typography variant="caption" color="text.secondary">
                  #{proposal.proposalId}
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>
                  {new Date(proposal.createdAt).toLocaleDateString('cs-CZ')}
                </Typography>
              </Box>

              <ListItemText
                primary={proposal.description}
                secondary={
                  <>
                    <Typography variant="caption" component="span">
                      Entity: <strong>{proposal.entity}</strong> | Autor: <strong>{proposal.author}</strong>
                    </Typography>
                    {proposal.reviewedBy && (
                      <Typography variant="caption" component="div" sx={{ mt: 0.5 }}>
                        Zkontroloval: <strong>{proposal.reviewedBy}</strong>
                        {proposal.reviewedAt && ` (${new Date(proposal.reviewedAt).toLocaleDateString('cs-CZ')})`}
                      </Typography>
                    )}
                  </>
                }
              />
            </ListItem>
            {index < proposals.length - 1 && <Divider />}
          </React.Fragment>
        ))}
      </List>
    </Box>
  );
}
