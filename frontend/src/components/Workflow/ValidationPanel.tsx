import { Box, Alert, AlertTitle, Chip, List, ListItem, ListItemIcon, ListItemText, Typography, Accordion, AccordionSummary, AccordionDetails } from '@mui/material';
import { 
  Error as ErrorIcon,
  Warning as WarningIcon,
  CheckCircle as SuccessIcon,
  ExpandMore as ExpandMoreIcon,
} from '@mui/icons-material';
import { tokens } from '../../shared/theme/tokens';

/**
 * W2: ValidationPanel - Zobrazení validation výsledků
 * 
 * Zobrazuje errors + warnings z WorkflowValidator
 */
export interface ValidationPanelProps {
  validationResult: {
    valid: boolean;
    errors: Array<{ code: string; message: string }>;
    warnings: Array<{ code: string; message: string }>;
  } | null;
  onClose?: () => void;
}

export const ValidationPanel = ({ validationResult, onClose }: ValidationPanelProps) => {
  if (!validationResult) {
    return null;
  }

  const { valid, errors, warnings } = validationResult;

  return (
    <Box sx={{ p: 2 }}>
      {/* Summary Alert */}
      <Alert 
        severity={valid ? 'success' : 'error'} 
        onClose={onClose}
        sx={{ mb: 2 }}
      >
        <AlertTitle>
          {valid ? '✅ Validace úspěšná' : '❌ Validace selhala'}
        </AlertTitle>
        <Typography variant="body2">
          {errors.length === 0 && warnings.length === 0 && 'Workflow je validní a připravený k použití.'}
          {errors.length > 0 && `Nalezeno ${errors.length} chyb`}
          {warnings.length > 0 && `, ${warnings.length} varování`}
        </Typography>
      </Alert>

      {/* Errors Section */}
      {errors.length > 0 && (
        <Accordion defaultExpanded sx={{ mb: 2 }}>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Box display="flex" alignItems="center" gap={1}>
              <ErrorIcon sx={{ color: tokens.colors.error[600] }} />
              <Typography fontWeight={600}>
                Chyby ({errors.length})
              </Typography>
            </Box>
          </AccordionSummary>
          <AccordionDetails>
            <List dense>
              {errors.map((error, index) => (
                <ListItem key={index} sx={{ pl: 0 }}>
                  <ListItemIcon>
                    <ErrorIcon sx={{ color: tokens.colors.error[600] }} />
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box display="flex" alignItems="center" gap={1}>
                        <Chip 
                          label={error.code} 
                          size="small" 
                          color="error"
                          variant="outlined"
                        />
                        <Typography variant="body2">{error.message}</Typography>
                      </Box>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </AccordionDetails>
        </Accordion>
      )}

      {/* Warnings Section */}
      {warnings.length > 0 && (
        <Accordion defaultExpanded={errors.length === 0}>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Box display="flex" alignItems="center" gap={1}>
              <WarningIcon sx={{ color: tokens.colors.warning[600] }} />
              <Typography fontWeight={600}>
                Varování ({warnings.length})
              </Typography>
            </Box>
          </AccordionSummary>
          <AccordionDetails>
            <List dense>
              {warnings.map((warning, index) => (
                <ListItem key={index} sx={{ pl: 0 }}>
                  <ListItemIcon>
                    <WarningIcon sx={{ color: tokens.colors.warning[600] }} />
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box display="flex" alignItems="center" gap={1}>
                        <Chip 
                          label={warning.code} 
                          size="small" 
                          color="warning"
                          variant="outlined"
                        />
                        <Typography variant="body2">{warning.message}</Typography>
                      </Box>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </AccordionDetails>
        </Accordion>
      )}

      {/* Success Icon */}
      {valid && errors.length === 0 && warnings.length === 0 && (
        <Box display="flex" justifyContent="center" py={2}>
          <SuccessIcon sx={{ fontSize: 64, color: tokens.colors.success[500] }} />
        </Box>
      )}
    </Box>
  );
};

export default ValidationPanel;
