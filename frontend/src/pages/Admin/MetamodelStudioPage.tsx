import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Grid,
  Alert,
  Tabs,
  Tab,
  Divider,
} from '@mui/material';
import { useAuth } from '../../components/AuthProvider.jsx';
import { ModelTree } from '../../components/Studio/ModelTree';
import { EntityDetail } from '../../components/Studio/EntityDetail';

interface Entity {
  name: string;
  entity: string;
  table: string;
  fields?: any[];
}

/**
 * S10-A: Metamodel Studio - Admin GUI for Metamodel Management
 * 
 * RBAC: Only CORE_ADMIN_STUDIO role can access
 * 
 * Layout:
 * - Left: ModelTree (entities, relations, validations)
 * - Center: Editor (entity/field/relation/validation editor)
 * - Right: Diff/Validation panel
 * 
 * Navigation tabs:
 * - Entities
 * - Relations
 * - Validations
 * - Workflow Steps (S10-E placeholder)
 */
export const MetamodelStudioPage = () => {
  const { user } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('entities');
  const [selectedEntity, setSelectedEntity] = useState<Entity | null>(null);

  // RBAC check - Cast user to any to avoid TS issues with roles property
  const hasAccess = (user as any)?.roles?.includes('CORE_ADMIN_STUDIO');

  useEffect(() => {
    if (!hasAccess) {
      setError('Přístup odepřen. Vyžadována role CORE_ADMIN_STUDIO.');
    }
  }, [hasAccess]);

  if (!hasAccess) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">
          <Typography variant="h6" gutterBottom>
            Přístup odepřen
          </Typography>
          <Typography variant="body2">
            Pro přístup do Metamodel Studio je vyžadována role <strong>CORE_ADMIN_STUDIO</strong>.
          </Typography>
        </Alert>
      </Box>
    );
  }

  const handleTabChange = (_event: React.SyntheticEvent, newValue: string) => {
    setActiveTab(newValue);
  };

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', background: '#f5f5f5' }}>
        <Typography variant="h4" gutterBottom>
          🎨 Metamodel Studio
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Admin GUI pro správu metamodelu, validací a workflow kroků
        </Typography>
      </Box>

      {/* Navigation Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', background: '#ffffff' }}>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="📦 Entities" value="entities" />
          <Tab label="🔗 Relations" value="relations" />
          <Tab label="✓ Validations" value="validations" />
          <Tab label="⚡ Workflow Steps" value="workflow-steps" disabled />
        </Tabs>
      </Box>

      {/* Main Content - 3 Column Layout */}
      <Box sx={{ flex: 1, overflow: 'hidden', p: 2, background: '#fafafa' }}>
        <Grid container spacing={2} sx={{ height: '100%' }}>
          {/* Left Panel: ModelTree */}
          <Grid item xs={12} md={3}>
            <Paper
              elevation={2}
              sx={{
                height: '100%',
                p: 2,
                overflow: 'auto',
                background: 'linear-gradient(to bottom, #f5f5f5, #ffffff)',
              }}
            >
              <Typography variant="h6" gutterBottom>
                📂 Model Tree
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <ModelTree
                onSelectEntity={setSelectedEntity}
                selectedEntity={selectedEntity}
              />
            </Paper>
          </Grid>

          {/* Center Panel: Editor */}
          <Grid item xs={12} md={6}>
            <Paper
              elevation={2}
              sx={{
                height: '100%',
                p: 2,
                overflow: 'auto',
              }}
            >
              <Typography variant="h6" gutterBottom>
                ✏️ Entity Detail (Read-only)
              </Typography>
              <Divider sx={{ mb: 2 }} />
              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {error}
                </Alert>
              )}
              <EntityDetail entity={selectedEntity} />
            </Paper>
          </Grid>

          {/* Right Panel: Diff/Validation */}
          <Grid item xs={12} md={3}>
            <Paper
              elevation={2}
              sx={{
                height: '100%',
                p: 2,
                overflow: 'auto',
                background: 'linear-gradient(to bottom, #fffbf0, #ffffff)',
              }}
            >
              <Typography variant="h6" gutterBottom>
                🔍 Diff & Validation
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Alert severity="info">
                S10-D: Diff viewer, Validate/Propose/Approve buttons
              </Alert>
            </Paper>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
};

export default MetamodelStudioPage;
