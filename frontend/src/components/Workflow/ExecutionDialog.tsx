/**
 * W7: Execution Result Dialog
 */

import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Chip,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
  Divider,
} from '@mui/material';
import {
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
  PlayArrow as StartIcon,
  AssignmentTurnedIn as TaskIcon,
  CallSplit as DecisionIcon,
  Stop as EndIcon,
  ArrowForward as ArrowIcon,
} from '@mui/icons-material';

interface ExecutionStep {
  nodeId: string;
  nodeType: string;
  label: string;
  result?: string;
  condition?: string;
  conditionResult?: boolean;
}

interface ExecutionResult {
  status: 'SUCCESS' | 'ERROR';
  steps: ExecutionStep[];
  durationMs: number;
  error?: string;
}

interface ExecutionDialogProps {
  open: boolean;
  onClose: () => void;
  result: ExecutionResult | null;
}

const getNodeIcon = (nodeType: string) => {
  switch (nodeType) {
    case 'start': return <StartIcon color="success" />;
    case 'task': return <TaskIcon color="primary" />;
    case 'decision': return <DecisionIcon color="warning" />;
    case 'end': return <EndIcon color="error" />;
    default: return <StartIcon />;
  }
};

export const ExecutionDialog = ({ open, onClose, result }: ExecutionDialogProps) => {
  if (!result) return null;

  const isSuccess = result.status === 'SUCCESS';

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box display="flex" alignItems="center" gap={2}>
          {isSuccess ? <SuccessIcon color="success" /> : <ErrorIcon color="error" />}
          <Typography variant="h6">Workflow Execution Result</Typography>
          <Chip 
            label={result.status} 
            color={isSuccess ? 'success' : 'error'} 
            size="small" 
          />
        </Box>
      </DialogTitle>

      <DialogContent dividers>
        {/* Duration */}
        <Box mb={3}>
          <Typography variant="body2" color="text.secondary">
            Duration: <strong>{result.durationMs}ms</strong>
          </Typography>
        </Box>

        {/* Error message */}
        {result.error && (
          <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: 'error.light', color: 'error.contrastText' }}>
            <Typography variant="body2">
              <strong>Error:</strong> {result.error}
            </Typography>
          </Paper>
        )}

        {/* Execution steps */}
        <Typography variant="h6" gutterBottom>
          Execution Steps
        </Typography>

        <List>
          {result.steps.map((step, index) => (
            <Box key={index}>
              <ListItem alignItems="flex-start">
                <ListItemIcon sx={{ mt: 1 }}>
                  {getNodeIcon(step.nodeType)}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" gap={1}>
                      <Typography variant="subtitle2" color="text.secondary">
                        {step.nodeType.toUpperCase()}
                      </Typography>
                      <Typography variant="body1" fontWeight="bold">
                        {step.label}
                      </Typography>
                    </Box>
                  }
                  secondary={
                    <Box mt={1}>
                      {/* Task result */}
                      {step.result && (
                        <Chip label={step.result} size="small" color="success" />
                      )}

                      {/* Decision result */}
                      {step.condition && (
                        <Box>
                          <Typography variant="body2" color="text.secondary" component="span">
                            Condition: <code>{step.condition}</code>
                          </Typography>
                          <Box display="inline-block" ml={1}>
                            <Chip 
                              label={step.conditionResult ? 'TRUE' : 'FALSE'} 
                              size="small" 
                              color={step.conditionResult ? 'success' : 'error'} 
                            />
                          </Box>
                        </Box>
                      )}
                    </Box>
                  }
                />
              </ListItem>
              {index < result.steps.length - 1 && <Divider variant="inset" component="li" />}
            </Box>
          ))}
        </List>
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} variant="contained">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ExecutionDialog;
