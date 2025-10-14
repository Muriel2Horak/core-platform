import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Grid,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Divider,
} from '@mui/material';
import { useAuth } from '../../components/AuthProvider.jsx';

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
export function MetamodelStudioPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('entities');

  // RBAC check
  const hasAccess = user?.roles?.includes('CORE_ADMIN_STUDIO');

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

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h4" gutterBottom>
          🎨 Metamodel Studio
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Admin GUI pro správu metamodelu, validací a workflow kroků
        </Typography>
      </Box>

      {/* Navigation Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="📦 Entities" value="entities" />
          <Tab label="🔗 Relations" value="relations" />
          <Tab label="✓ Validations" value="validations" />
          <Tab label="⚡ Workflow Steps" value="workflow-steps" disabled />
        </Tabs>
      </Box>

      {/* Main Content - 3 Column Layout */}
      <Box sx={{ flex: 1, overflow: 'hidden', p: 2 }}>
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
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                  <CircularProgress size={40} />
                </Box>
              ) : (
                <Alert severity="info" sx={{ mt: 2 }}>
                  S10-B: Tree view bude načítat entity z BE
                </Alert>
              )}
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
                ✏️ Editor
              </Typography>
              <Divider sx={{ mb: 2 }} />
              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {error}
                </Alert>
              )}
              <Alert severity="info">
                S10-C: EntityEditor, FieldEditor, RelationEditor, ValidationEditor
              </Alert>
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
}

export default MetamodelStudioPage;
