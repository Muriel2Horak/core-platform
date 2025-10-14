import { useState, useEffect, useCallback, useRef } from 'react';
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
  Tooltip,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import UndoIcon from '@mui/icons-material/Undo';
import RedoIcon from '@mui/icons-material/Redo';
import SaveIcon from '@mui/icons-material/Save';
import DownloadIcon from '@mui/icons-material/Download';
import UploadIcon from '@mui/icons-material/Upload';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';

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
 * S10-C+F: EntityEditor - Edit entity schema with UX enhancements
 * 
 * S10-C Features:
 * - Edit entity name, table name
 * - Edit fields (name, type, required, unique)
 * - Validate draft (client + server)
 * - Save draft locally
 * 
 * S10-F Features (UX Enhancements):
 * - Undo/Redo history (max 50 entries)
 * - Autosave with debounce (500ms)
 * - Export/Import draft JSON
 * - Duplicate entity/field buttons
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

  // S10-F: Undo/Redo history
  const [history, setHistory] = useState<EntityDraft[]>([]);
  const [historyIndex, setHistoryIndex] = useState(-1);
  const MAX_HISTORY = 50;

  // S10-F: Autosave indicator
  const [lastSaved, setLastSaved] = useState<string | null>(null);
  const autosaveTimerRef = useRef<NodeJS.Timeout | null>(null);

  // S10-F: File upload ref
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (entity) {
      const newDraft = {
        entity: entity.entity || entity.name || '',
        table: entity.table || '',
        idField: entity.idField || 'id',
        versionField: entity.versionField || 'version',
        tenantField: entity.tenantField || 'tenant_id',
        fields: entity.fields || [],
      };
      setDraft(newDraft);
      setValidationStatus('idle');
      setValidationErrors([]);
      // Initialize history with first draft
      setHistory([newDraft]);
      setHistoryIndex(0);
    }
  }, [entity]);

  // S10-F: Autosave with debounce (500ms)
  useEffect(() => {
    if (autosaveTimerRef.current) {
      clearTimeout(autosaveTimerRef.current);
    }

    autosaveTimerRef.current = setTimeout(() => {
      try {
        localStorage.setItem(`draft-${draft.entity}`, JSON.stringify(draft));
        setLastSaved(new Date().toLocaleTimeString());
      } catch (err) {
        console.error('Autosave failed:', err);
      }
    }, 500);

    return () => {
      if (autosaveTimerRef.current) {
        clearTimeout(autosaveTimerRef.current);
      }
    };
  }, [draft]);

  // S10-F: Add to history on draft change
  const addToHistory = useCallback((newDraft: EntityDraft) => {
    setHistory((prev) => {
      // Remove any "future" history if we're not at the end
      const newHistory = prev.slice(0, historyIndex + 1);
      // Add new draft
      newHistory.push(newDraft);
      // Limit to MAX_HISTORY
      if (newHistory.length > MAX_HISTORY) {
        newHistory.shift();
        setHistoryIndex((prevIndex) => prevIndex); // Keep same relative position
      } else {
        setHistoryIndex((prevIndex) => prevIndex + 1);
      }
      return newHistory;
    });
  }, [historyIndex, MAX_HISTORY]);

  const handleFieldChange = (field: keyof EntityDraft, value: any) => {
    const newDraft = { ...draft, [field]: value };
    setDraft(newDraft);
    addToHistory(newDraft);
    setValidationStatus('idle');
  };

  const handleFieldItemChange = (index: number, field: keyof Field, value: any) => {
    const newFields = [...draft.fields];
    newFields[index] = { ...newFields[index], [field]: value };
    const newDraft = { ...draft, fields: newFields };
    setDraft(newDraft);
    addToHistory(newDraft);
    setValidationStatus('idle');
  };

  const handleAddField = () => {
    const newDraft = {
      ...draft,
      fields: [...draft.fields, { name: '', type: 'string', required: false, unique: false }],
    };
    setDraft(newDraft);
    addToHistory(newDraft);
  };

  const handleRemoveField = (index: number) => {
    const newDraft = {
      ...draft,
      fields: draft.fields.filter((_, i) => i !== index),
    };
    setDraft(newDraft);
    addToHistory(newDraft);
  };

  // S10-F: Duplicate field
  const handleDuplicateField = (index: number) => {
    const field = draft.fields[index];
    const newDraft = {
      ...draft,
      fields: [
        ...draft.fields,
        { ...field, name: `${field.name}_copy` },
      ],
    };
    setDraft(newDraft);
    addToHistory(newDraft);
  };

  // S10-F: Duplicate entity
  const handleDuplicateEntity = () => {
    const newDraft = {
      ...draft,
      entity: `${draft.entity}Copy`,
      table: `${draft.table}_copy`,
    };
    setDraft(newDraft);
    addToHistory(newDraft);
  };

  // S10-F: Undo
  const handleUndo = () => {
    if (historyIndex > 0) {
      setHistoryIndex((prev) => prev - 1);
      setDraft(history[historyIndex - 1]);
      setValidationStatus('idle');
    }
  };

  // S10-F: Redo
  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      setHistoryIndex((prev) => prev + 1);
      setDraft(history[historyIndex + 1]);
      setValidationStatus('idle');
    }
  };

  // S10-F: Export draft as JSON
  const handleExport = () => {
    const dataStr = JSON.stringify(draft, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${draft.entity || 'entity'}-draft.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  // S10-F: Import draft from JSON
  const handleImport = () => {
    fileInputRef.current?.click();
  };

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const imported = JSON.parse(e.target?.result as string);
        setDraft(imported);
        addToHistory(imported);
        setValidationStatus('idle');
      } catch (err) {
        alert('Failed to parse JSON file');
      }
    };
    reader.readAsText(file);
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
      {/* S10-F: Toolbar with UX actions */}
      <Paper variant="outlined" sx={{ p: 1, mb: 2, display: 'flex', gap: 1, alignItems: 'center' }}>
        <Tooltip title="Undo (Ctrl+Z)">
          <span>
            <IconButton size="small" onClick={handleUndo} disabled={historyIndex <= 0}>
              <UndoIcon fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="Redo (Ctrl+Y)">
          <span>
            <IconButton size="small" onClick={handleRedo} disabled={historyIndex >= history.length - 1}>
              <RedoIcon fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
        <Divider orientation="vertical" flexItem />
        <Tooltip title="Duplicate Entity">
          <IconButton size="small" onClick={handleDuplicateEntity}>
            <ContentCopyIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title="Export Draft JSON">
          <IconButton size="small" onClick={handleExport}>
            <DownloadIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title="Import Draft JSON">
          <IconButton size="small" onClick={handleImport}>
            <UploadIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <input
          type="file"
          ref={fileInputRef}
          accept=".json"
          style={{ display: 'none' }}
          onChange={handleFileUpload}
        />
        <Box sx={{ flexGrow: 1 }} />
        {lastSaved && (
          <Chip
            icon={<SaveIcon />}
            label={`Saved ${lastSaved}`}
            size="small"
            color="success"
            variant="outlined"
          />
        )}
      </Paper>

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
                  <Tooltip title="Duplicate Field">
                    <IconButton size="small" onClick={() => handleDuplicateField(index)}>
                      <ContentCopyIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
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
