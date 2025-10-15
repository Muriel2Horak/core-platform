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
  Button,
  Stack,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { useAuth } from '../../components/AuthProvider.jsx';
import { ModelTree } from '../../components/Studio/ModelTree';
import { EntityDetail } from '../../components/Studio/EntityDetail';
import { EntityEditor } from '../../components/Studio/EntityEditor';
import { DiffPanel } from '../../components/Studio/DiffPanel';
import { WorkflowStepsEditor } from '../../components/Studio/WorkflowStepsEditor';
import { AiConfigEditor } from '../../components/Studio/AiConfigEditor';
import { AiHelpWidget } from '../../components/AiHelpWidget';

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
  const routeId = 'admin.studio.metamodel';
  const [selectedEntity, setSelectedEntity] = useState<Entity | null>(null);
  const [editMode, setEditMode] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [draft, setDraft] = useState<any>(null);
  const [workflowSteps, setWorkflowSteps] = useState<any[]>([]);

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

  const handleSaveDraft = (draftData: any) => {
    console.log('💾 Saving draft:', draftData);
    setDraft(draftData);
    setSuccessMessage('Draft saved! Preview diff to see changes.');
    setTimeout(() => setSuccessMessage(null), 3000);
  };

  const handleValidate = (draftData: any) => {
    console.log('✓ Validated:', draftData);
  };

  const handlePropose = (proposalId: string) => {
    setSuccessMessage(`Proposal ${proposalId} created! Check Proposals tab.`);
    setTimeout(() => setSuccessMessage(null), 5000);
  };

  const handlePublish = () => {
    setSuccessMessage('Hot reload successful! Metamodel updated.');
    setDraft(null); // Clear draft after publish
    setTimeout(() => setSuccessMessage(null), 5000);
  };

  const handleValidateWorkflowSteps = async (steps: any[]) => {
    try {
      const response = await fetch('/api/admin/studio/workflow-steps/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ steps }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      return await response.json();
    } catch (err: any) {
      return { valid: false, errors: [{ stepId: '', field: '', message: err.message }] };
    }
  };

  const handleDryRunWorkflowSteps = async (steps: any[], context: any) => {
    try {
      const response = await fetch('/api/admin/studio/workflow-steps/dry-run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ steps, context }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      return await response.json();
    } catch (err: any) {
      return { success: false, steps: [{ stepId: '', status: 'ERROR', error: err.message }] };
    }
  };

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }} data-route-id={routeId}>
      {/* Header */}
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', background: '#f5f5f5' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Typography variant="h4" gutterBottom>
              🎨 Metamodel Studio
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Admin GUI pro správu metamodelu, validací a workflow kroků
            </Typography>
          </Box>
          <Box display="flex" gap={1}>
            <AiHelpWidget routeId={routeId} />
            {selectedEntity && (
              <Stack direction="row" spacing={1}>
                <Button
                  variant={editMode ? 'outlined' : 'contained'}
                  startIcon={<VisibilityIcon />}
                  size="small"
                  onClick={() => setEditMode(false)}
                >
                  View
                </Button>
                <Button
                  variant={editMode ? 'contained' : 'outlined'}
                  startIcon={<EditIcon />}
                  size="small"
                  onClick={() => setEditMode(true)}
                >
                  Edit
                </Button>
              </Stack>
            )}
          </Box>
        </Box>
      </Box>

      {/* Navigation Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', background: '#ffffff' }}>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="📦 Entities" value="entities" />
          <Tab label="🔗 Relations" value="relations" disabled />
          <Tab label="✓ Validations" value="validations" disabled />
          <Tab label="⚡ Workflow Steps" value="workflow-steps" />
          <Tab label="🤖 AI Config" value="ai-config" />
        </Tabs>
      </Box>

      {/* Main Content - 3 Column Layout OR Workflow Steps Editor OR AI Config Editor */}
      <Box sx={{ flex: 1, overflow: 'hidden', p: 2, background: '#fafafa' }}>
        {activeTab === 'workflow-steps' ? (
          /* Full-width Workflow Steps Editor */
          <Paper
            elevation={2}
            sx={{
              height: '100%',
              p: 3,
              overflow: 'auto',
            }}
          >
            <WorkflowStepsEditor
              steps={workflowSteps}
              onChange={setWorkflowSteps}
              onValidate={handleValidateWorkflowSteps}
              onDryRun={handleDryRunWorkflowSteps}
            />
          </Paper>
        ) : activeTab === 'ai-config' ? (
          /* Full-width AI Config Editor */
          <Paper
            elevation={2}
            sx={{
              height: '100%',
              p: 3,
              overflow: 'auto',
            }}
          >
            <AiConfigEditor
              onSave={(config) => {
                console.log('AI config saved:', config);
                setSuccessMessage('AI configuration saved successfully!');
                setTimeout(() => setSuccessMessage(null), 3000);
              }}
              readOnly={!(user as any)?.roles?.some((r: string) =>
                ['PLATFORM_ADMIN', 'OPS'].includes(r)
              )}
            />
          </Paper>
        ) : (
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

            {/* Center Panel: Editor / Detail */}
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
                  {editMode ? '✏️ Entity Editor' : '📋 Entity Detail (Read-only)'}
                </Typography>
                <Divider sx={{ mb: 2 }} />
                {error && (
                  <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                  </Alert>
                )}
                {successMessage && (
                  <Alert severity="success" sx={{ mb: 2 }}>
                    {successMessage}
                  </Alert>
                )}
                {editMode ? (
                  <EntityEditor
                    entity={selectedEntity}
                    onSave={handleSaveDraft}
                    onValidate={handleValidate}
                  />
                ) : (
                  <EntityDetail entity={selectedEntity} />
                )}
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
                <DiffPanel
                  entity={selectedEntity}
                  draft={draft}
                  onPropose={handlePropose}
                  onPublish={handlePublish}
                />
              </Paper>
            </Grid>
          </Grid>
        )}
      </Box>
    </Box>
  );
};

export default MetamodelStudioPage;
