import { useState, useEffect } from 'react';
import {
  Box,
  TextField,
  Button,
  Alert,
  Typography,
  Paper,
  Divider,
  CircularProgress,
  Stack,
  IconButton,
  Chip,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';

interface Field {
  name: string;
  column?: string;
  type: string;
  required?: boolean;
  unique?: boolean;
}

interface EntityDraft {
  entity: string;
  table: string;
  idField?: string;
  versionField?: string;
  tenantField?: string;
  fields: Field[];
}

interface ValidationError {
  field: string;
  message: string;
  severity: 'error' | 'warning' | 'info';
}

interface EntityEditorProps {
  entity: any;
  onSave?: (draft: EntityDraft) => void;
  onValidate?: (draft: EntityDraft) => void;
}

/**
 * S10-C: EntityEditor - Edit entity schema (draft mode)
 * 
 * Features:
 * - Edit entity name, table name
 * - Edit fields (name, type, required, unique)
 * - Validate draft (client + server)
 * - Save draft locally
 */
export function EntityEditor({ entity, onSave, onValidate }: EntityEditorProps) {
  const [draft, setDraft] = useState<EntityDraft>({
    entity: '',
    table: '',
    idField: 'id',
    versionField: 'version',
    tenantField: 'tenant_id',
    fields: [],
  });

  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);
  const [validating, setValidating] = useState(false);
  const [validationStatus, setValidationStatus] = useState<'idle' | 'valid' | 'invalid'>('idle');

  useEffect(() => {
    if (entity) {
      setDraft({
        entity: entity.entity || entity.name || '',
        table: entity.table || '',
        idField: entity.idField || 'id',
        versionField: entity.versionField || 'version',
        tenantField: entity.tenantField || 'tenant_id',
        fields: entity.fields || [],
      });
      setValidationStatus('idle');
      setValidationErrors([]);
    }
  }, [entity]);

  const handleFieldChange = (field: keyof EntityDraft, value: any) => {
    setDraft((prev) => ({ ...prev, [field]: value }));
    setValidationStatus('idle');
  };

  const handleFieldItemChange = (index: number, field: keyof Field, value: any) => {
    const newFields = [...draft.fields];
    newFields[index] = { ...newFields[index], [field]: value };
    setDraft((prev) => ({ ...prev, fields: newFields }));
    setValidationStatus('idle');
  };

  const handleAddField = () => {
    setDraft((prev) => ({
      ...prev,
      fields: [...prev.fields, { name: '', type: 'string', required: false, unique: false }],
    }));
  };

  const handleRemoveField = (index: number) => {
    setDraft((prev) => ({
      ...prev,
      fields: prev.fields.filter((_, i) => i !== index),
    }));
  };

  const handleValidate = async () => {
    setValidating(true);
    setValidationErrors([]);

    try {
      const response = await fetch('/api/admin/studio/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(draft),
      });

      const result = await response.json();

      if (result.status === 'valid') {
        setValidationStatus('valid');
        setValidationErrors([]);
        if (onValidate) onValidate(draft);
      } else {
        setValidationStatus('invalid');
        setValidationErrors(result.errors || []);
      }
    } catch (err: any) {
      setValidationStatus('invalid');
      setValidationErrors([
        { field: 'general', message: err.message || 'Validation failed', severity: 'error' },
      ]);
    } finally {
      setValidating(false);
    }
  };

  const handleSave = () => {
    if (onSave) onSave(draft);
  };

  const hasErrors = validationErrors.some((e) => e.severity === 'error');

  return (
    <Box>
      {/* Entity metadata */}
      <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Typography variant="h6" gutterBottom>
          Entity Metadata
        </Typography>
        <Stack spacing={2}>
          <TextField
            label="Entity Name"
            value={draft.entity}
            onChange={(e) => handleFieldChange('entity', e.target.value)}
            size="small"
            fullWidth
            helperText="PascalCase (e.g., User, OrderItem)"
          />
          <TextField
            label="Table Name"
            value={draft.table}
            onChange={(e) => handleFieldChange('table', e.target.value)}
            size="small"
            fullWidth
            helperText="snake_case (e.g., users, order_items)"
          />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField
              label="ID Field"
              value={draft.idField}
              onChange={(e) => handleFieldChange('idField', e.target.value)}
              size="small"
              fullWidth
            />
            <TextField
              label="Version Field"
              value={draft.versionField}
              onChange={(e) => handleFieldChange('versionField', e.target.value)}
              size="small"
              fullWidth
            />
            <TextField
              label="Tenant Field"
              value={draft.tenantField}
              onChange={(e) => handleFieldChange('tenantField', e.target.value)}
              size="small"
              fullWidth
            />
          </Box>
        </Stack>
      </Paper>

      {/* Fields */}
      <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Fields ({draft.fields.length})</Typography>
          <Button startIcon={<AddIcon />} size="small" onClick={handleAddField}>
            Add Field
          </Button>
        </Box>
        <Divider sx={{ mb: 2 }} />
        <Stack spacing={2}>
          {draft.fields.map((field, index) => (
            <Paper key={index} variant="outlined" sx={{ p: 2 }}>
              <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
                <TextField
                  label="Name"
                  value={field.name}
                  onChange={(e) => handleFieldItemChange(index, 'name', e.target.value)}
                  size="small"
                  sx={{ flex: 1 }}
                />
                <TextField
                  label="Type"
                  value={field.type}
                  onChange={(e) => handleFieldItemChange(index, 'type', e.target.value)}
                  size="small"
                  sx={{ flex: 1 }}
                />
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                  <Chip
                    label="Required"
                    color={field.required ? 'error' : 'default'}
                    size="small"
                    onClick={() => handleFieldItemChange(index, 'required', !field.required)}
                  />
                  <Chip
                    label="Unique"
                    color={field.unique ? 'primary' : 'default'}
                    size="small"
                    onClick={() => handleFieldItemChange(index, 'unique', !field.unique)}
                  />
                  <IconButton size="small" color="error" onClick={() => handleRemoveField(index)}>
                    <DeleteIcon />
                  </IconButton>
                </Box>
              </Box>
            </Paper>
          ))}
        </Stack>
      </Paper>

      {/* Validation errors */}
      {validationErrors.length > 0 && (
        <Alert severity="error" sx={{ mb: 2 }}>
          <Typography variant="subtitle2" gutterBottom>
            Validation Errors:
          </Typography>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            {validationErrors.map((err, idx) => (
              <li key={idx}>
                <strong>{err.field}:</strong> {err.message}
              </li>
            ))}
          </ul>
        </Alert>
      )}

      {/* Validation success */}
      {validationStatus === 'valid' && (
        <Alert severity="success" icon={<CheckIcon />} sx={{ mb: 2 }}>
          Validation passed! Draft is ready to propose.
        </Alert>
      )}

      {/* Actions */}
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
        <Button
          variant="outlined"
          onClick={handleValidate}
          disabled={validating}
          startIcon={validating ? <CircularProgress size={16} /> : <CheckIcon />}
        >
          {validating ? 'Validating...' : 'Validate'}
        </Button>
        <Button
          variant="contained"
          onClick={handleSave}
          disabled={validationStatus !== 'valid' || hasErrors}
        >
          Save Draft
        </Button>
      </Box>
    </Box>
  );
}

export default EntityEditor;
