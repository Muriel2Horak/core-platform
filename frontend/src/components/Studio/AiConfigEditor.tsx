import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  TextField,
  Switch,
  FormControlLabel,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
  Alert,
  Divider,
  Chip,
  Stack,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SaveIcon from '@mui/icons-material/Save';
import RefreshIcon from '@mui/icons-material/Refresh';
import AddIcon from '@mui/icons-material/Add';
import InfoIcon from '@mui/icons-material/Info';

interface AiConfigEditorProps {
  onSave?: (config: any) => void;
  readOnly?: boolean;
}

/**
 * AI Configuration Editor for Metamodel Studio
 * 
 * Step F: Admin nastavení
 * 
 * RBAC:
 * - PlatformAdmin/Ops: Full write access
 * - TenantAdmin: Read-only view
 * 
 * Features:
 * - Global AI toggle (AI_ENABLED)
 * - AI mode selection (META_ONLY enforced)
 * - Redact fields patterns
 * - System prompts for userAgent/devAgent
 * - Entity/route level field editors (pii, helpSafe, mask)
 * - MCP tools configuration
 */
export const AiConfigEditor: React.FC<AiConfigEditorProps> = ({ onSave, readOnly = false }) => {
  const [config, setConfig] = useState<any>({
    enabled: false,
    mode: 'META_ONLY',
    policies: {
      defaultVisibility: 'META_ONLY',
      redactFields: ['password', 'secret', 'token'],
      limits: {
        maxFields: 30,
        maxRecords: 20,
        maxTokens: 8000,
      },
    },
    prompts: {
      userAgent: 'You are a helpful assistant for business users. Provide clear, actionable guidance based on metadata only.',
      devAgent: 'You are a technical assistant for developers. Explain workflows, validations, and data structures.',
    },
    tools: [],
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [newRedactField, setNewRedactField] = useState('');

  useEffect(() => {
    loadConfig();
  }, []);

  const loadConfig = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/admin/ai/config');
      if (!response.ok) {
        throw new Error(`Failed to load config: ${response.status}`);
      }
      const data = await response.json();
      setConfig(data);
    } catch (err: any) {
      setError(err.message);
      console.error('Failed to load AI config:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await fetch('/api/admin/ai/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config),
      });

      if (!response.ok) {
        throw new Error(`Failed to save config: ${response.status}`);
      }

      setSuccess('AI configuration saved successfully!');
      setTimeout(() => setSuccess(null), 3000);
      onSave?.(config);
    } catch (err: any) {
      setError(err.message);
      console.error('Failed to save AI config:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddRedactField = () => {
    if (newRedactField && !config.policies.redactFields.includes(newRedactField)) {
      setConfig({
        ...config,
        policies: {
          ...config.policies,
          redactFields: [...config.policies.redactFields, newRedactField],
        },
      });
      setNewRedactField('');
    }
  };

  const handleRemoveRedactField = (field: string) => {
    setConfig({
      ...config,
      policies: {
        ...config.policies,
        redactFields: config.policies.redactFields.filter((f: string) => f !== field),
      },
    });
  };

  return (
    <Box sx={{ height: '100%', overflow: 'auto' }}>
      <Stack spacing={2}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h5">🤖 AI Configuration</Typography>
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={loadConfig}
              disabled={loading}
            >
              Reload
            </Button>
            <Button
              variant="contained"
              startIcon={<SaveIcon />}
              onClick={handleSave}
              disabled={loading || readOnly}
            >
              Save
            </Button>
          </Stack>
        </Box>

        {/* Alerts */}
        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">{success}</Alert>}
        {readOnly && (
          <Alert severity="info">
            <strong>Read-only mode:</strong> You don't have permission to edit AI configuration.
            Contact PlatformAdmin or Ops.
          </Alert>
        )}

        {/* Global Settings */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">⚙️ Global Settings</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Stack spacing={2}>
              <FormControlLabel
                control={
                  <Switch
                    checked={config.enabled}
                    onChange={(e) => setConfig({ ...config, enabled: e.target.checked })}
                    disabled={readOnly}
                  />
                }
                label={
                  <Box>
                    <Typography variant="body1">
                      AI Enabled (Kill-switch)
                      <Tooltip title="When disabled, all AI endpoints return 404 and help widgets are hidden">
                        <InfoIcon fontSize="small" sx={{ ml: 1, verticalAlign: 'middle' }} />
                      </Tooltip>
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {config.enabled ? '✅ AI features are active' : '⛔ AI features are disabled'}
                    </Typography>
                  </Box>
                }
              />

              <FormControl fullWidth>
                <InputLabel>AI Mode</InputLabel>
                <Select
                  value={config.mode}
                  label="AI Mode"
                  disabled={true} // Always META_ONLY in this version
                >
                  <MenuItem value="META_ONLY">META_ONLY (No data values, metadata only)</MenuItem>
                  <MenuItem value="REDACTED" disabled>REDACTED (PII masked) - Not implemented</MenuItem>
                  <MenuItem value="FULL" disabled>FULL (All data) - Not implemented</MenuItem>
                </Select>
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
                  <strong>Note:</strong> Only META_ONLY is supported. Even if FULL is requested, system enforces META_ONLY.
                </Typography>
              </FormControl>

              <FormControl fullWidth>
                <InputLabel>Default Visibility</InputLabel>
                <Select
                  value={config.policies.defaultVisibility}
                  label="Default Visibility"
                  onChange={(e) =>
                    setConfig({
                      ...config,
                      policies: { ...config.policies, defaultVisibility: e.target.value },
                    })
                  }
                  disabled={readOnly}
                >
                  <MenuItem value="META_ONLY">META_ONLY</MenuItem>
                  <MenuItem value="REDACTED" disabled>REDACTED</MenuItem>
                  <MenuItem value="FULL" disabled>FULL</MenuItem>
                </Select>
              </FormControl>
            </Stack>
          </AccordionDetails>
        </Accordion>

        {/* Policies */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">🔒 Policies & Limits</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Stack spacing={3}>
              {/* Redact Fields */}
              <Box>
                <Typography variant="subtitle1" gutterBottom>
                  Redact Fields (PII Patterns)
                </Typography>
                <Typography variant="caption" color="text.secondary" gutterBottom>
                  Field names matching these patterns will be marked as PII
                </Typography>
                <Box sx={{ mt: 1, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {config.policies.redactFields.map((field: string) => (
                    <Chip
                      key={field}
                      label={field}
                      onDelete={readOnly ? undefined : () => handleRemoveRedactField(field)}
                      color="primary"
                      variant="outlined"
                    />
                  ))}
                </Box>
                {!readOnly && (
                  <Box sx={{ mt: 2, display: 'flex', gap: 1 }}>
                    <TextField
                      size="small"
                      placeholder="e.g. email, phone, ssn"
                      value={newRedactField}
                      onChange={(e) => setNewRedactField(e.target.value)}
                      onKeyPress={(e) => e.key === 'Enter' && handleAddRedactField()}
                    />
                    <Button
                      variant="outlined"
                      startIcon={<AddIcon />}
                      onClick={handleAddRedactField}
                    >
                      Add
                    </Button>
                  </Box>
                )}
              </Box>

              <Divider />

              {/* Limits */}
              <Box>
                <Typography variant="subtitle1" gutterBottom>
                  Limits
                </Typography>
                <Stack spacing={2}>
                  <TextField
                    label="Max Fields per Entity"
                    type="number"
                    value={config.policies.limits.maxFields}
                    onChange={(e) =>
                      setConfig({
                        ...config,
                        policies: {
                          ...config.policies,
                          limits: { ...config.policies.limits, maxFields: parseInt(e.target.value) },
                        },
                      })
                    }
                    disabled={readOnly}
                    helperText="Maximum number of fields to include in AI context"
                  />
                  <TextField
                    label="Max Records per Query"
                    type="number"
                    value={config.policies.limits.maxRecords}
                    onChange={(e) =>
                      setConfig({
                        ...config,
                        policies: {
                          ...config.policies,
                          limits: { ...config.policies.limits, maxRecords: parseInt(e.target.value) },
                        },
                      })
                    }
                    disabled={readOnly}
                    helperText="Maximum number of records to return (future use)"
                  />
                  <TextField
                    label="Max Tokens"
                    type="number"
                    value={config.policies.limits.maxTokens}
                    onChange={(e) =>
                      setConfig({
                        ...config,
                        policies: {
                          ...config.policies,
                          limits: { ...config.policies.limits, maxTokens: parseInt(e.target.value) },
                        },
                      })
                    }
                    disabled={readOnly}
                    helperText="Maximum tokens for AI context (estimated)"
                  />
                </Stack>
              </Box>
            </Stack>
          </AccordionDetails>
        </Accordion>

        {/* System Prompts */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">💬 System Prompts</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Stack spacing={2}>
              <TextField
                label="User Agent Prompt"
                multiline
                rows={4}
                value={config.prompts.userAgent}
                onChange={(e) =>
                  setConfig({
                    ...config,
                    prompts: { ...config.prompts, userAgent: e.target.value },
                  })
                }
                disabled={readOnly}
                helperText="System prompt for business user AI assistant"
                fullWidth
              />
              <TextField
                label="Developer Agent Prompt"
                multiline
                rows={4}
                value={config.prompts.devAgent}
                onChange={(e) =>
                  setConfig({
                    ...config,
                    prompts: { ...config.prompts, devAgent: e.target.value },
                  })
                }
                disabled={readOnly}
                helperText="System prompt for developer/technical AI assistant"
                fullWidth
              />
            </Stack>
          </AccordionDetails>
        </Accordion>

        {/* MCP Tools (Read-only overview) */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">🔧 MCP Tools</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              MCP (Model Context Protocol) tools available for AI agents:
            </Typography>
            <Table size="small" sx={{ mt: 2 }}>
              <TableHead>
                <TableRow>
                  <TableCell><strong>Tool Name</strong></TableCell>
                  <TableCell><strong>Description</strong></TableCell>
                  <TableCell><strong>Status</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                <TableRow>
                  <TableCell><code>ui_context.get_current_view</code></TableCell>
                  <TableCell>Get current UI screen metadata</TableCell>
                  <TableCell><Chip label="Active" color="success" size="small" /></TableCell>
                </TableRow>
                <TableRow>
                  <TableCell><code>wf_context.get_workflow</code></TableCell>
                  <TableCell>Get workflow states & transitions</TableCell>
                  <TableCell><Chip label="Active" color="success" size="small" /></TableCell>
                </TableRow>
                <TableRow>
                  <TableCell><code>auth.get_user_capabilities</code></TableCell>
                  <TableCell>Get user permissions (stub)</TableCell>
                  <TableCell><Chip label="Stub" color="warning" size="small" /></TableCell>
                </TableRow>
                <TableRow>
                  <TableCell><code>data_context.query</code></TableCell>
                  <TableCell>Query data (not implemented)</TableCell>
                  <TableCell><Chip label="Not Impl" color="error" size="small" /></TableCell>
                </TableRow>
              </TableBody>
            </Table>
            <Alert severity="info" sx={{ mt: 2 }}>
              MCP tools are configured at code level. Future versions may allow dynamic tool registration.
            </Alert>
          </AccordionDetails>
        </Accordion>

        {/* Field Editor Placeholder */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">📝 Entity/Route Field Configuration</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Alert severity="info">
              <Typography variant="body2">
                <strong>Coming soon:</strong> Entity and route-level field editors for:
              </Typography>
              <ul>
                <li><code>pii: true/false</code> - Mark PII fields</li>
                <li><code>helpSafe: true/false</code> - Safe to expose in help mode</li>
                <li><code>mask: "pattern"</code> - Masking pattern (e.g., "u***@d***.cz")</li>
              </ul>
              <Typography variant="body2">
                For now, edit these directly in <code>metamodel/*.yaml</code> files.
              </Typography>
            </Alert>
          </AccordionDetails>
        </Accordion>
      </Stack>
    </Box>
  );
};

export default AiConfigEditor;
