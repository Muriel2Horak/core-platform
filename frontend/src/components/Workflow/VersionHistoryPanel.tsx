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
import { CheckCircle, Archive } from '@mui/icons-material';

interface WorkflowVersion {
  versionId: number;
  entity: string;
  proposalId: number;
  createdBy: string;
  createdAt: string;
  status: 'ACTIVE' | 'SUPERSEDED';
}

interface VersionHistoryPanelProps {
  entity: string;
  refreshTrigger?: number;
}

/**
 * W3: Panel for displaying workflow version history
 */
export function VersionHistoryPanel({ entity, refreshTrigger }: VersionHistoryPanelProps) {
  const [versions, setVersions] = useState<WorkflowVersion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadVersions = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/admin/workflows/${entity}/versions`);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      setVersions(data);
    } catch (err) {
      console.error('Failed to load versions:', err);
      setError('Nepodařilo se načíst verze');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadVersions();
  }, [entity, refreshTrigger]);

  const getStatusIcon = (versionStatus: string) => {
    return versionStatus === 'ACTIVE' ? (
      <CheckCircle fontSize="small" />
    ) : (
      <Archive fontSize="small" />
    );
  };

  const getStatusColor = (versionStatus: string) => {
    return versionStatus === 'ACTIVE' ? 'success' : 'default';
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
        <Button onClick={loadVersions} sx={{ mt: 2 }} fullWidth>
          Zkusit znovu
        </Button>
      </Box>
    );
  }

  if (versions.length === 0) {
    return (
      <Box sx={{ p: 3, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          Žádné verze
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Vytvořte první návrh a schvalte ho pro vytvoření verze
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ maxHeight: '100%', overflow: 'auto' }}>
      <Box sx={{ p: 2, pb: 1 }}>
        <Typography variant="subtitle2" gutterBottom>
          Historie verzí: <strong>{entity}</strong>
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Celkem verzí: {versions.length}
        </Typography>
      </Box>
      
      <List>
        {versions.map((version, index) => (
          <React.Fragment key={version.versionId}>
            <ListItem
              sx={{
                flexDirection: 'column',
                alignItems: 'flex-start',
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1, width: '100%' }}>
                <Chip
                  icon={getStatusIcon(version.status)}
                  label={version.status}
                  color={getStatusColor(version.status) as any}
                  size="small"
                />
                <Typography variant="caption" color="text.secondary">
                  v{version.versionId}
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>
                  {new Date(version.createdAt).toLocaleDateString('cs-CZ', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </Typography>
              </Box>

              <ListItemText
                primary={`Vytvořil: ${version.createdBy}`}
                secondary={`Z návrhu #${version.proposalId}`}
                primaryTypographyProps={{ variant: 'body2' }}
                secondaryTypographyProps={{ variant: 'caption' }}
              />
            </ListItem>
            {index < versions.length - 1 && <Divider />}
          </React.Fragment>
        ))}
      </List>
    </Box>
  );
}
