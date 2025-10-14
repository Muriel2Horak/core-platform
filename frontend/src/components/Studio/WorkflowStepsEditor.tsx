import { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  IconButton,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Stack,
  Chip,
  Alert,
  Divider,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

interface WorkflowStep {
  id: string;
  type: 'rest' | 'kafka' | 'email' | 'custom';
  label: string;
  actionCode: string;
  inputMap: Record<string, string>;
  onSuccess?: string;
  onError?: string;
  retry?: {
    maxAttempts: number;
    initialDelayMs: number;
    maxDelayMs: number;
    backoffMultiplier: number;
  };
  timeout?: number;
  compensate?: string;
  openapiRef?: string;
  asyncapiRef?: string;
  kafka?: {
    topic: string;
    key?: string;
  };
  correlation?: string;
}

interface WorkflowStepsEditorProps {
  steps: WorkflowStep[];
  onChange: (steps: WorkflowStep[]) => void;
  onValidate?: (steps: WorkflowStep[]) => Promise<ValidationResult>;
  onDryRun?: (steps: WorkflowStep[], context: Record<string, any>) => Promise<DryRunResult>;
}

interface ValidationResult {
  valid: boolean;
  errors: Array<{ stepId: string; field: string; message: string }>;
}

interface DryRunResult {
  success: boolean;
  steps: Array<{ stepId: string; status: string; output?: any; error?: string }>;
}

/**
 * S10-E: WorkflowStepsEditor - Editor pro workflow.steps[] schéma
 * 
 * Features:
 * - Add/remove/edit workflow steps
 * - Type selection: REST, Kafka, Email, Custom
 * - InputMap editor (key-value pairs)
 * - Retry policy configuration
 * - Timeout/compensate/correlation fields
 * - Validation (client + server)
 * - Dry-run testing with mock context
 */
export function WorkflowStepsEditor({
  steps,
  onChange,
  onValidate,
  onDryRun,
}: WorkflowStepsEditorProps) {
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const [dryRunResult, setDryRunResult] = useState<DryRunResult | null>(null);
  const [validating, setValidating] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testContext, setTestContext] = useState<string>('{}');

  const handleAddStep = () => {
    const newStep: WorkflowStep = {
      id: `step-${Date.now()}`,
      type: 'rest',
      label: 'New Step',
      actionCode: '',
      inputMap: {},
    };
    onChange([...steps, newStep]);
  };

  const handleRemoveStep = (id: string) => {
    onChange(steps.filter((s) => s.id !== id));
  };

  const handleUpdateStep = (id: string, updates: Partial<WorkflowStep>) => {
    onChange(
      steps.map((s) => (s.id === id ? { ...s, ...updates } : s))
    );
  };

  const handleAddInputMapEntry = (stepId: string) => {
    const step = steps.find((s) => s.id === stepId);
    if (!step) return;

    const newInputMap = {
      ...step.inputMap,
      [`key${Object.keys(step.inputMap).length + 1}`]: '',
    };
    handleUpdateStep(stepId, { inputMap: newInputMap });
  };

  const handleUpdateInputMapEntry = (
    stepId: string,
    oldKey: string,
    newKey: string,
    value: string
  ) => {
    const step = steps.find((s) => s.id === stepId);
    if (!step) return;

    const newInputMap = { ...step.inputMap };
    if (oldKey !== newKey) {
      delete newInputMap[oldKey];
    }
    newInputMap[newKey] = value;
    handleUpdateStep(stepId, { inputMap: newInputMap });
  };

  const handleRemoveInputMapEntry = (stepId: string, key: string) => {
    const step = steps.find((s) => s.id === stepId);
    if (!step) return;

    const newInputMap = { ...step.inputMap };
    delete newInputMap[key];
    handleUpdateStep(stepId, { inputMap: newInputMap });
  };

  const handleValidate = async () => {
    if (!onValidate) return;

    setValidating(true);
    setValidationResult(null);

    try {
      const result = await onValidate(steps);
      setValidationResult(result);
    } catch (err: any) {
      setValidationResult({
        valid: false,
        errors: [{ stepId: '', field: '', message: err.message || 'Validation failed' }],
      });
    } finally {
      setValidating(false);
    }
  };

  const handleDryRun = async () => {
    if (!onDryRun) return;

    setTesting(true);
    setDryRunResult(null);

    try {
      const context = JSON.parse(testContext);
      const result = await onDryRun(steps, context);
      setDryRunResult(result);
    } catch (err: any) {
      setDryRunResult({
        success: false,
        steps: [{ stepId: '', status: 'ERROR', error: err.message || 'Dry run failed' }],
      });
    } finally {
      setTesting(false);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">⚡ Workflow Steps Editor</Typography>
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            startIcon={<CheckCircleIcon />}
            size="small"
            onClick={handleValidate}
            disabled={validating || steps.length === 0}
          >
            Validate
          </Button>
          <Button
            variant="outlined"
            startIcon={<PlayArrowIcon />}
            size="small"
            onClick={handleDryRun}
            disabled={testing || steps.length === 0}
          >
            Dry Run
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            size="small"
            onClick={handleAddStep}
          >
            Add Step
          </Button>
        </Stack>
      </Box>

      <Divider sx={{ mb: 2 }} />

      {/* Validation Results */}
      {validationResult && (
        <Alert
          severity={validationResult.valid ? 'success' : 'error'}
          sx={{ mb: 2 }}
          onClose={() => setValidationResult(null)}
        >
          {validationResult.valid ? (
            'All steps are valid!'
          ) : (
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Validation Errors:
              </Typography>
              {validationResult.errors.map((err, idx) => (
                <Typography key={idx} variant="caption" display="block">
                  • Step {err.stepId}: {err.field} - {err.message}
                </Typography>
              ))}
            </Box>
          )}
        </Alert>
      )}

      {/* Dry Run Results */}
      {dryRunResult && (
        <Paper variant="outlined" sx={{ p: 2, mb: 2, bgcolor: '#f5f5f5' }}>
          <Typography variant="subtitle2" gutterBottom>
            🧪 Dry Run Results:
          </Typography>
          {dryRunResult.steps.map((step, idx) => (
            <Box key={idx} sx={{ mb: 1 }}>
              <Chip
                label={step.status}
                size="small"
                color={step.status === 'SUCCESS' ? 'success' : 'error'}
                sx={{ mr: 1 }}
              />
              <Typography variant="caption">
                Step {step.stepId}: {step.error || 'OK'}
              </Typography>
              {step.output && (
                <pre style={{ fontSize: '0.7rem', margin: '4px 0', overflow: 'auto' }}>
                  {JSON.stringify(step.output, null, 2)}
                </pre>
              )}
            </Box>
          ))}
        </Paper>
      )}

      {/* Steps List */}
      {steps.length === 0 && (
        <Alert severity="info">No workflow steps defined. Click "Add Step" to create one.</Alert>
      )}

      <Stack spacing={2}>
        {steps.map((step, idx) => (
          <Accordion key={step.id} defaultExpanded>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, width: '100%' }}>
                <Chip label={`#${idx + 1}`} size="small" />
                <Chip label={step.type} size="small" color="primary" />
                <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
                  {step.label || '(Untitled)'}
                </Typography>
                <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRemoveStep(step.id);
                  }}
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Box>
            </AccordionSummary>
            <AccordionDetails>
              <Stack spacing={2}>
                {/* Basic Fields */}
                <TextField
                  label="Label"
                  value={step.label}
                  onChange={(e) => handleUpdateStep(step.id, { label: e.target.value })}
                  fullWidth
                  size="small"
                />

                <FormControl fullWidth size="small">
                  <InputLabel>Type</InputLabel>
                  <Select
                    value={step.type}
                    onChange={(e) =>
                      handleUpdateStep(step.id, {
                        type: e.target.value as WorkflowStep['type'],
                      })
                    }
                  >
                    <MenuItem value="rest">REST API</MenuItem>
                    <MenuItem value="kafka">Kafka Event</MenuItem>
                    <MenuItem value="email">Email</MenuItem>
                    <MenuItem value="custom">Custom</MenuItem>
                  </Select>
                </FormControl>

                <TextField
                  label="Action Code"
                  value={step.actionCode}
                  onChange={(e) => handleUpdateStep(step.id, { actionCode: e.target.value })}
                  fullWidth
                  size="small"
                  helperText="Unique identifier for this action (e.g., 'send-approval-email')"
                />

                {/* Type-specific fields */}
                {step.type === 'rest' && (
                  <TextField
                    label="OpenAPI Ref"
                    value={step.openapiRef || ''}
                    onChange={(e) => handleUpdateStep(step.id, { openapiRef: e.target.value })}
                    fullWidth
                    size="small"
                    helperText="Reference to OpenAPI operation (e.g., '/api/users#POST')"
                  />
                )}

                {step.type === 'kafka' && (
                  <>
                    <TextField
                      label="AsyncAPI Ref"
                      value={step.asyncapiRef || ''}
                      onChange={(e) => handleUpdateStep(step.id, { asyncapiRef: e.target.value })}
                      fullWidth
                      size="small"
                    />
                    <TextField
                      label="Kafka Topic"
                      value={step.kafka?.topic || ''}
                      onChange={(e) =>
                        handleUpdateStep(step.id, {
                          kafka: { ...step.kafka, topic: e.target.value },
                        })
                      }
                      fullWidth
                      size="small"
                    />
                    <TextField
                      label="Kafka Key (optional)"
                      value={step.kafka?.key || ''}
                      onChange={(e) =>
                        handleUpdateStep(step.id, {
                          kafka: { topic: step.kafka?.topic || '', key: e.target.value },
                        })
                      }
                      fullWidth
                      size="small"
                    />
                  </>
                )}

                {/* InputMap Editor */}
                <Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="caption" color="text.secondary">
                      Input Mapping
                    </Typography>
                    <Button
                      size="small"
                      startIcon={<AddIcon />}
                      onClick={() => handleAddInputMapEntry(step.id)}
                    >
                      Add Entry
                    </Button>
                  </Box>
                  <Stack spacing={1}>
                    {Object.entries(step.inputMap).map(([key, value]) => (
                      <Box key={key} sx={{ display: 'flex', gap: 1 }}>
                        <TextField
                          label="Key"
                          value={key}
                          onChange={(e) =>
                            handleUpdateInputMapEntry(step.id, key, e.target.value, value)
                          }
                          size="small"
                          sx={{ flexBasis: '40%' }}
                        />
                        <TextField
                          label="Value"
                          value={value}
                          onChange={(e) =>
                            handleUpdateInputMapEntry(step.id, key, key, e.target.value)
                          }
                          size="small"
                          sx={{ flexGrow: 1 }}
                        />
                        <IconButton
                          size="small"
                          onClick={() => handleRemoveInputMapEntry(step.id, key)}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    ))}
                  </Stack>
                </Box>

                {/* Flow Control */}
                <TextField
                  label="On Success (next step ID)"
                  value={step.onSuccess || ''}
                  onChange={(e) => handleUpdateStep(step.id, { onSuccess: e.target.value })}
                  fullWidth
                  size="small"
                />

                <TextField
                  label="On Error (next step ID)"
                  value={step.onError || ''}
                  onChange={(e) => handleUpdateStep(step.id, { onError: e.target.value })}
                  fullWidth
                  size="small"
                />

                <TextField
                  label="Compensate (step ID)"
                  value={step.compensate || ''}
                  onChange={(e) => handleUpdateStep(step.id, { compensate: e.target.value })}
                  fullWidth
                  size="small"
                  helperText="Step to run if this step needs rollback"
                />

                <TextField
                  label="Correlation ID"
                  value={step.correlation || ''}
                  onChange={(e) => handleUpdateStep(step.id, { correlation: e.target.value })}
                  fullWidth
                  size="small"
                  helperText="Expression for correlation ID (e.g., '${orderId}')"
                />

                {/* Advanced Settings */}
                <Accordion>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="caption">Advanced: Retry & Timeout</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <Stack spacing={2}>
                      <TextField
                        label="Timeout (ms)"
                        type="number"
                        value={step.timeout || ''}
                        onChange={(e) =>
                          handleUpdateStep(step.id, { timeout: parseInt(e.target.value, 10) })
                        }
                        fullWidth
                        size="small"
                      />

                      <Typography variant="caption" color="text.secondary">
                        Retry Policy
                      </Typography>

                      <TextField
                        label="Max Attempts"
                        type="number"
                        value={step.retry?.maxAttempts || 3}
                        onChange={(e) =>
                          handleUpdateStep(step.id, {
                            retry: {
                              ...step.retry,
                              maxAttempts: parseInt(e.target.value, 10),
                              initialDelayMs: step.retry?.initialDelayMs || 1000,
                              maxDelayMs: step.retry?.maxDelayMs || 30000,
                              backoffMultiplier: step.retry?.backoffMultiplier || 2.0,
                            },
                          })
                        }
                        fullWidth
                        size="small"
                      />

                      <TextField
                        label="Initial Delay (ms)"
                        type="number"
                        value={step.retry?.initialDelayMs || 1000}
                        onChange={(e) =>
                          handleUpdateStep(step.id, {
                            retry: {
                              ...step.retry,
                              initialDelayMs: parseInt(e.target.value, 10),
                              maxAttempts: step.retry?.maxAttempts || 3,
                              maxDelayMs: step.retry?.maxDelayMs || 30000,
                              backoffMultiplier: step.retry?.backoffMultiplier || 2.0,
                            },
                          })
                        }
                        fullWidth
                        size="small"
                      />

                      <TextField
                        label="Max Delay (ms)"
                        type="number"
                        value={step.retry?.maxDelayMs || 30000}
                        onChange={(e) =>
                          handleUpdateStep(step.id, {
                            retry: {
                              ...step.retry,
                              maxDelayMs: parseInt(e.target.value, 10),
                              maxAttempts: step.retry?.maxAttempts || 3,
                              initialDelayMs: step.retry?.initialDelayMs || 1000,
                              backoffMultiplier: step.retry?.backoffMultiplier || 2.0,
                            },
                          })
                        }
                        fullWidth
                        size="small"
                      />

                      <TextField
                        label="Backoff Multiplier"
                        type="number"
                        inputProps={{ step: 0.1 }}
                        value={step.retry?.backoffMultiplier || 2.0}
                        onChange={(e) =>
                          handleUpdateStep(step.id, {
                            retry: {
                              ...step.retry,
                              backoffMultiplier: parseFloat(e.target.value),
                              maxAttempts: step.retry?.maxAttempts || 3,
                              initialDelayMs: step.retry?.initialDelayMs || 1000,
                              maxDelayMs: step.retry?.maxDelayMs || 30000,
                            },
                          })
                        }
                        fullWidth
                        size="small"
                      />
                    </Stack>
                  </AccordionDetails>
                </Accordion>
              </Stack>
            </AccordionDetails>
          </Accordion>
        ))}
      </Stack>

      {/* Dry Run Context Editor */}
      {steps.length > 0 && (
        <Paper variant="outlined" sx={{ p: 2, mt: 2 }}>
          <Typography variant="caption" color="text.secondary" gutterBottom display="block">
            Test Context (JSON)
          </Typography>
          <TextField
            value={testContext}
            onChange={(e) => setTestContext(e.target.value)}
            multiline
            rows={4}
            fullWidth
            size="small"
            placeholder='{"orderId": "123", "amount": 1500}'
            sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}
          />
        </Paper>
      )}
    </Box>
  );
}

export default WorkflowStepsEditor;
