import { Box, Button, ButtonGroup, Divider, Tooltip, Typography } from '@mui/material';
import {
  Add as AddIcon,
  AccountTree as LayoutIcon,
  Save as SaveIcon,
  FolderOpen as LoadIcon,
  PlayArrow as StartIcon,
  CheckCircle as EndIcon,
  CallSplit as DecisionIcon,
  Undo as UndoIcon,
  Redo as RedoIcon,
  Check as ValidateIcon,
  PlayCircleOutline as SimulateIcon,
} from '@mui/icons-material';
import { tokens } from '../../shared/theme/tokens';

/**
 * W1-W2: WorkflowToolbar - Toolbar pro Workflow Designer
 * 
 * Akce:
 * - Add nodes (Start, Task, Decision, End)
 * - Layout (ELK hierarchical, Dagre simple)
 * - Save/Load draft
 * - Validate (W2)
 * - Simulate (W2)
 * - Undo/Redo (future)
 */
export interface WorkflowToolbarProps {
  onAddNode: (type: 'start' | 'task' | 'decision' | 'end') => void;
  onAutoLayout: (engine: 'elk' | 'dagre') => void;
  onSave: () => void;
  onLoad: () => void;
  onValidate?: () => void; // W2
  onSimulate?: () => void; // W2
  canUndo?: boolean;
  canRedo?: boolean;
  onUndo?: () => void;
  onRedo?: () => void;
}

export const WorkflowToolbar = ({
  onAddNode,
  onAutoLayout,
  onSave,
  onLoad,
  onValidate,
  onSimulate,
  canUndo = false,
  canRedo = false,
  onUndo,
  onRedo,
}: WorkflowToolbarProps) => {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 2,
        p: 2,
        backgroundColor: tokens.colors.grey[50],
        borderBottom: `1px solid ${tokens.colors.grey[200]}`,
        borderRadius: `${tokens.borderRadius.md} ${tokens.borderRadius.md} 0 0`,
      }}
    >
      {/* Add Nodes Group */}
      <Box display="flex" alignItems="center" gap={1}>
        <Typography variant="caption" fontWeight={600} color="text.secondary">
          Přidat:
        </Typography>
        <ButtonGroup size="small" variant="outlined">
          <Tooltip title="Přidat START state">
            <Button
              startIcon={<StartIcon />}
              onClick={() => onAddNode('start')}
              sx={{ color: tokens.colors.success[600] }}
            >
              Start
            </Button>
          </Tooltip>
          <Tooltip title="Přidat TASK state">
            <Button
              startIcon={<AddIcon />}
              onClick={() => onAddNode('task')}
              sx={{ color: tokens.colors.primary[600] }}
            >
              Task
            </Button>
          </Tooltip>
          <Tooltip title="Přidat DECISION state">
            <Button
              startIcon={<DecisionIcon />}
              onClick={() => onAddNode('decision')}
              sx={{ color: tokens.colors.warning[600] }}
            >
              Decision
            </Button>
          </Tooltip>
          <Tooltip title="Přidat END state">
            <Button
              startIcon={<EndIcon />}
              onClick={() => onAddNode('end')}
              sx={{ color: tokens.colors.error[600] }}
            >
              End
            </Button>
          </Tooltip>
        </ButtonGroup>
      </Box>

      <Divider orientation="vertical" flexItem />

      {/* Layout Group */}
      <Box display="flex" alignItems="center" gap={1}>
        <Typography variant="caption" fontWeight={600} color="text.secondary">
          Layout:
        </Typography>
        <ButtonGroup size="small" variant="outlined">
          <Tooltip title="ELK hierarchical layout">
            <Button
              startIcon={<LayoutIcon />}
              onClick={() => onAutoLayout('elk')}
            >
              ELK
            </Button>
          </Tooltip>
          <Tooltip title="Dagre simple layout">
            <Button
              startIcon={<LayoutIcon />}
              onClick={() => onAutoLayout('dagre')}
            >
              Dagre
            </Button>
          </Tooltip>
        </ButtonGroup>
      </Box>

      <Divider orientation="vertical" flexItem />

      {/* Validate & Simulate (W2) */}
      {(onValidate || onSimulate) && (
        <>
          <Box display="flex" gap={1}>
            {onValidate && (
              <Tooltip title="Validovat workflow">
                <Button
                  size="small"
                  variant="outlined"
                  color="success"
                  startIcon={<ValidateIcon />}
                  onClick={onValidate}
                >
                  Validovat
                </Button>
              </Tooltip>
            )}
            {onSimulate && (
              <Tooltip title="Simulovat workflow">
                <Button
                  size="small"
                  variant="outlined"
                  color="info"
                  startIcon={<SimulateIcon />}
                  onClick={onSimulate}
                >
                  Simulovat
                </Button>
              </Tooltip>
            )}
          </Box>
          <Divider orientation="vertical" flexItem />
        </>
      )}

      {/* Save/Load Group */}
      <Box display="flex" gap={1}>
        <Tooltip title="Uložit koncept">
          <Button
            size="small"
            variant="contained"
            startIcon={<SaveIcon />}
            onClick={onSave}
          >
            Uložit
          </Button>
        </Tooltip>
        <Tooltip title="Načíst koncept">
          <Button
            size="small"
            variant="outlined"
            startIcon={<LoadIcon />}
            onClick={onLoad}
          >
            Načíst
          </Button>
        </Tooltip>
      </Box>

      <Box flexGrow={1} />

      {/* Undo/Redo Group (future) */}
      <Box display="flex" gap={1}>
        <Tooltip title={canUndo ? 'Zpět' : 'Nelze vrátit zpět'}>
          <span>
            <Button
              size="small"
              variant="text"
              startIcon={<UndoIcon />}
              onClick={onUndo}
              disabled={!canUndo}
            >
              Zpět
            </Button>
          </span>
        </Tooltip>
        <Tooltip title={canRedo ? 'Znovu' : 'Nelze opakovat'}>
          <span>
            <Button
              size="small"
              variant="text"
              startIcon={<RedoIcon />}
              onClick={onRedo}
              disabled={!canRedo}
            >
              Znovu
            </Button>
          </span>
        </Tooltip>
      </Box>
    </Box>
  );
};

export default WorkflowToolbar;
