import {
  Box,
  Typography,
  Divider,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Alert,
} from '@mui/material';

interface Entity {
  name: string;
  entity: string;
  table: string;
  idField?: string;
  versionField?: string;
  tenantField?: string;
  fields?: any[];
  accessPolicy?: any;
  ui?: any;
  navigation?: any;
  features?: any[];
}

interface EntityDetailProps {
  entity: Entity | null;
}

/**
 * S10-B: EntityDetail component - Read-only viewer for entity schema
 * 
 * Displays:
 * - Entity metadata (name, table, idField, etc.)
 * - Fields table (name, type, required, unique)
 * - Access policy
 * - UI config
 * - Navigation config
 */
export function EntityDetail({ entity }: EntityDetailProps) {
  if (!entity) {
    return (
      <Alert severity="info">
        Vyberte entitu ze seznamu vlevo
      </Alert>
    );
  }

  return (
    <Box>
      {/* Entity header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" gutterBottom>
          {entity.name}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Tabulka: <strong>{entity.table}</strong>
        </Typography>
      </Box>

      <Divider sx={{ mb: 3 }} />

      {/* Metadata section */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          📋 Metadata
        </Typography>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                ID Field:
              </Typography>
              <Typography variant="body2">{entity.idField || 'N/A'}</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Version Field:
              </Typography>
              <Typography variant="body2">{entity.versionField || 'N/A'}</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Tenant Field:
              </Typography>
              <Typography variant="body2">{entity.tenantField || 'N/A'}</Typography>
            </Box>
          </Box>
        </Paper>
      </Box>

      {/* Fields table */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          📦 Fields ({entity.fields?.length || 0})
        </Typography>
        {entity.fields && entity.fields.length > 0 ? (
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell><strong>Name</strong></TableCell>
                  <TableCell><strong>Column</strong></TableCell>
                  <TableCell><strong>Type</strong></TableCell>
                  <TableCell><strong>Flags</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {entity.fields.map((field: any, idx: number) => (
                  <TableRow key={idx}>
                    <TableCell>{field.name}</TableCell>
                    <TableCell>{field.column || field.name}</TableCell>
                    <TableCell>
                      <Chip label={field.type} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>
                      {field.required && (
                        <Chip label="Required" size="small" color="error" sx={{ mr: 0.5 }} />
                      )}
                      {field.unique && (
                        <Chip label="Unique" size="small" color="primary" sx={{ mr: 0.5 }} />
                      )}
                      {field.searchable && (
                        <Chip label="Searchable" size="small" color="info" sx={{ mr: 0.5 }} />
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Alert severity="info">Žádná pole</Alert>
        )}
      </Box>

      {/* Access Policy */}
      {entity.accessPolicy && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            🔒 Access Policy
          </Typography>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <pre style={{ margin: 0, fontSize: '0.75rem', overflow: 'auto' }}>
              {JSON.stringify(entity.accessPolicy, null, 2)}
            </pre>
          </Paper>
        </Box>
      )}

      {/* UI Config */}
      {entity.ui && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            🎨 UI Config
          </Typography>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <pre style={{ margin: 0, fontSize: '0.75rem', overflow: 'auto' }}>
              {JSON.stringify(entity.ui, null, 2)}
            </pre>
          </Paper>
        </Box>
      )}

      {/* Navigation Config */}
      {entity.navigation && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            🧭 Navigation
          </Typography>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <pre style={{ margin: 0, fontSize: '0.75rem', overflow: 'auto' }}>
              {JSON.stringify(entity.navigation, null, 2)}
            </pre>
          </Paper>
        </Box>
      )}
    </Box>
  );
}

export default EntityDetail;
