import { Box, Typography, Stepper, Step, StepLabel, StepContent, Chip, Alert, Paper } from '@mui/material';
import { 
  PlayCircle as StartIcon,
  CheckCircle as EndIcon,
  AccountTree as TaskIcon,
  CallSplit as DecisionIcon,
} from '@mui/icons-material';
import { tokens } from '../../shared/theme/tokens';

/**
 * W2: SimulationPanel - Zobrazení simulation trace
 * 
 * Zobrazuje step-by-step execution trace workflow simulace
 */
export interface SimulationPanelProps {
  simulationResult: {
    success: boolean;
    trace: Array<{
      stepNumber: number;
      nodeId: string;
      nodeType: string;
      nodeLabel: string;
      dataSnapshot: Record<string, unknown>;
    }>;
    message: string;
  } | null;
}

const getNodeIcon = (nodeType: string) => {
  switch (nodeType?.toUpperCase()) {
    case 'START':
      return <StartIcon sx={{ color: tokens.colors.success[600] }} />;
    case 'END':
      return <EndIcon sx={{ color: tokens.colors.error[600] }} />;
    case 'DECISION':
      return <DecisionIcon sx={{ color: tokens.colors.warning[600] }} />;
    case 'TASK':
    default:
      return <TaskIcon sx={{ color: tokens.colors.primary[600] }} />;
  }
};

const getNodeColor = (nodeType: string) => {
  switch (nodeType?.toUpperCase()) {
    case 'START':
      return tokens.colors.success[100];
    case 'END':
      return tokens.colors.error[100];
    case 'DECISION':
      return tokens.colors.warning[100];
    case 'TASK':
    default:
      return tokens.colors.primary[100];
  }
};

export const SimulationPanel = ({ simulationResult }: SimulationPanelProps) => {
  if (!simulationResult) {
    return null;
  }

  const { success, trace, message } = simulationResult;

  return (
    <Box sx={{ p: 2 }}>
      {/* Summary Alert */}
      <Alert 
        severity={success ? 'success' : 'error'} 
        sx={{ mb: 3 }}
      >
        <Typography variant="body2" fontWeight={600}>
          {message}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Celkem kroků: {trace.length}
        </Typography>
      </Alert>

      {/* Execution Trace */}
      {trace.length > 0 && (
        <Paper 
          elevation={0} 
          sx={{ 
            p: 2, 
            backgroundColor: tokens.colors.grey[50],
            maxHeight: 500,
            overflow: 'auto',
          }}
        >
          <Stepper orientation="vertical" activeStep={trace.length}>
            {trace.map((step, index) => (
              <Step key={index} active={true} completed={index < trace.length - 1}>
                <StepLabel
                  StepIconComponent={() => (
                    <Box
                      sx={{
                        width: 32,
                        height: 32,
                        borderRadius: '50%',
                        backgroundColor: getNodeColor(step.nodeType),
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      {getNodeIcon(step.nodeType)}
                    </Box>
                  )}
                >
                  <Box display="flex" alignItems="center" gap={1}>
                    <Typography variant="body2" fontWeight={600}>
                      {step.nodeLabel}
                    </Typography>
                    <Chip 
                      label={step.nodeType} 
                      size="small" 
                      sx={{ fontSize: 10, height: 20 }}
                    />
                  </Box>
                </StepLabel>
                <StepContent>
                  <Box sx={{ pl: 2, pb: 2 }}>
                    <Typography variant="caption" color="text.secondary">
                      Krok #{step.stepNumber} • Node ID: {step.nodeId}
                    </Typography>
                    {Object.keys(step.dataSnapshot).length > 0 && (
                      <Box sx={{ mt: 1 }}>
                        <Typography variant="caption" color="text.secondary">
                          Data snapshot:
                        </Typography>
                        <Paper 
                          variant="outlined" 
                          sx={{ 
                            p: 1, 
                            mt: 0.5, 
                            backgroundColor: 'white',
                            fontSize: 11,
                            fontFamily: 'monospace',
                          }}
                        >
                          {JSON.stringify(step.dataSnapshot, null, 2)}
                        </Paper>
                      </Box>
                    )}
                  </Box>
                </StepContent>
              </Step>
            ))}
          </Stepper>
        </Paper>
      )}
    </Box>
  );
};

export default SimulationPanel;
