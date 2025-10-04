import { useState, useMemo } from 'react';
/**
 * 📋 KanbanPage - Example page with dnd-kit Kanban Board
 * 
 * Ukázka implementace Kanban board s funkcemi:
 * - Drag & drop cards mezi sloupci
 * - Reordering cards v rámci sloupce
 * - Add/edit/delete cards
 * - Responsive design
 */
import PropTypes from 'prop-types';
import { TaskPropType, ColumnPropType } from '../../shared/propTypes.js';
import {
  Box,
  Paper,
  Typography,
  Card,
  CardContent,
  Button,
  Avatar,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import {
  DndContext,
  DragOverlay,
  closestCorners,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import {
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { WorkSection } from '../../shared/ui/WorkArea';
import { tokens } from '../../shared/theme/tokens';

// 🎲 Mock data pro ukázku
const initialColumns = [
  { id: 'todo', title: 'K udělání', color: '#e3f2fd' },
  { id: 'in-progress', title: 'Probíhá', color: '#fff3e0' },
  { id: 'review', title: 'Review', color: '#f3e5f5' },
  { id: 'done', title: 'Hotovo', color: '#e8f5e8' },
];

const initialTasks = [
  {
    id: '1',
    title: 'Návrh UI komponent',
    description: 'Vytvořit návrhy pro nové UI komponenty podle design systému',
    assignee: 'Jan Novák',
    priority: 'high',
    columnId: 'todo',
  },
  {
    id: '2',
    title: 'Implementace API',
    description: 'Implementovat REST API pro správu uživatelů',
    assignee: 'Marie Svobodová',
    priority: 'medium',
    columnId: 'in-progress',
  },
  {
    id: '3',
    title: 'Code review',
    description: 'Provést code review pro nové funkce',
    assignee: 'Petr Dvořák',
    priority: 'low',
    columnId: 'review',
  },
  {
    id: '4',
    title: 'Aktualizace dokumentace',
    description: 'Aktualizovat dokumentaci projektu',
    assignee: 'Eva Procházková',
    priority: 'medium',
    columnId: 'done',
  },
  {
    id: '5',
    title: 'Testování nových funkcí',
    description: 'Otestovat všechny nové funkce před releae',
    assignee: 'Tomáš Černý',
    priority: 'high',
    columnId: 'todo',
  },
];

// 📋 Task Card komponenta
const TaskCard = ({ task, isDragging = false }) => {
  const [anchorEl, setAnchorEl] = useState(null);
  
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging: isSortableDragging,
  } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isSortableDragging ? 0.5 : 1,
  };

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'high': return 'error';
      case 'medium': return 'warning';
      case 'low': return 'success';
      default: return 'default';
    }
  };

  const getInitials = (name) => {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  };

  return (
    <Card
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      sx={{
        mb: 2,
        cursor: isDragging ? 'grabbing' : 'grab',
        transform: isDragging ? 'rotate(5deg)' : 'none',
        boxShadow: isDragging ? tokens.shadows.xl : tokens.shadows.sm,
        '&:hover': {
          boxShadow: tokens.shadows.md,
        },
        transition: 'all 0.2s ease',
      }}
    >
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, flex: 1 }}>
            {task.title}
          </Typography>
          <IconButton
            size="small"
            onClick={(e) => {
              e.stopPropagation();
              setAnchorEl(e.currentTarget);
            }}
            sx={{ ml: 1 }}
          >
            <MoreVertIcon fontSize="small" />
          </IconButton>
        </Box>
        
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2, fontSize: '0.8rem' }}>
          {task.description}
        </Typography>
        
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Avatar sx={{ width: 24, height: 24, fontSize: '0.7rem' }}>
              {getInitials(task.assignee)}
            </Avatar>
            <Typography variant="caption" color="text.secondary">
              {task.assignee}
            </Typography>
          </Box>
          
          <Chip
            size="small"
            label={task.priority}
            color={getPriorityColor(task.priority)}
            sx={{ height: 20, fontSize: '0.65rem' }}
          />
        </Box>
        
        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={() => setAnchorEl(null)}
          onClick={(e) => e.stopPropagation()}
        >
          <MenuItem onClick={() => setAnchorEl(null)}>
            <EditIcon sx={{ mr: 1 }} fontSize="small" />
            Upravit
          </MenuItem>
          <MenuItem onClick={() => setAnchorEl(null)}>
            <DeleteIcon sx={{ mr: 1 }} fontSize="small" />
            Smazat
          </MenuItem>
        </Menu>
      </CardContent>
    </Card>
  );
};

TaskCard.propTypes = {
  task: TaskPropType.isRequired,
  isDragging: PropTypes.bool,
};

// 📂 Kanban Column komponenta
const KanbanColumn = ({ column, tasks }) => {
  const {
    setNodeRef,
    isOver,
  } = useSortable({
    id: column.id,
    data: {
      type: 'Column',
      column,
    },
  });

  return (
    <Paper
      ref={setNodeRef}
      sx={{
        minWidth: 300,
        maxWidth: 300,
        height: 'fit-content',
        maxHeight: 'calc(100vh - 200px)',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: isOver ? 'action.hover' : 'background.paper',
        border: isOver ? `2px dashed ${tokens.colors.primary[300]}` : '1px solid transparent',
        transition: 'all 0.2s ease',
      }}
    >
      {/* Column Header */}
      <Box sx={{ 
        p: 2, 
        backgroundColor: column.color,
        borderRadius: '4px 4px 0 0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            {column.title}
          </Typography>
          <Chip 
            label={tasks.length} 
            size="small" 
            sx={{ height: 20, fontSize: '0.7rem' }}
          />
        </Box>
        
        <Button
          size="small"
          startIcon={<AddIcon />}
          variant="text"
          sx={{ minWidth: 'auto', p: 0.5 }}
        >
          <AddIcon fontSize="small" />
        </Button>
      </Box>
      
      {/* Tasks */}
      <Box sx={{ 
        p: 2, 
        flex: 1,
        overflowY: 'auto',
        minHeight: 100,
      }}>
        <SortableContext items={tasks} strategy={verticalListSortingStrategy}>
          {tasks.map((task) => (
            <TaskCard key={task.id} task={task} />
          ))}
        </SortableContext>
      </Box>
    </Paper>
  );
};

KanbanColumn.propTypes = {
  column: ColumnPropType.isRequired,
  tasks: PropTypes.arrayOf(TaskPropType).isRequired,
};

export default function KanbanPage() {
  const [tasks, setTasks] = useState(initialTasks);
  const [columns] = useState(initialColumns);
  const [activeId, setActiveId] = useState(null);
  const [addDialogOpen, setAddDialogOpen] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  // Seskupení úkolů podle sloupců
  const tasksByColumn = useMemo(() => {
    return columns.reduce((acc, column) => {
      acc[column.id] = tasks.filter(task => task.columnId === column.id);
      return acc;
    }, {});
  }, [tasks, columns]);

  const handleDragStart = (event) => {
    setActiveId(event.active.id);
  };

  const handleDragOver = (event) => {
    const { active, over } = event;
    
    if (!over) return;

    const activeId = active.id;
    const overId = over.id;

    if (activeId === overId) return;

    const isActiveATask = tasks.find(task => task.id === activeId);
    const isOverATask = tasks.find(task => task.id === overId);
    
    if (!isActiveATask) return;

    // Přesouvání mezi sloupci
    if (isActiveATask && !isOverATask) {
      const overColumn = columns.find(col => col.id === overId);
      if (overColumn) {
        setTasks((tasks) => {
          return tasks.map(task => {
            if (task.id === activeId) {
              return { ...task, columnId: overId };
            }
            return task;
          });
        });
      }
    }
  };

  const handleDragEnd = (event) => {
    const { active, over } = event;
    
    if (!over) {
      setActiveId(null);
      return;
    }

    const activeId = active.id;
    const overId = over.id;

    if (activeId === overId) {
      setActiveId(null);
      return;
    }

    const isActiveATask = tasks.find(task => task.id === activeId);
    const isOverATask = tasks.find(task => task.id === overId);

    if (isActiveATask && isOverATask) {
      // Reordering within same column or between columns
      setTasks((tasks) => {
        const activeIndex = tasks.findIndex(task => task.id === activeId);
        const overIndex = tasks.findIndex(task => task.id === overId);

        const activeTask = tasks[activeIndex];
        const overTask = tasks[overIndex];

        if (activeTask.columnId !== overTask.columnId) {
          // Move to different column
          activeTask.columnId = overTask.columnId;
        }

        return arrayMove(tasks, activeIndex, overIndex);
      });
    }

    setActiveId(null);
  };

  const activeTask = activeId ? tasks.find(task => task.id === activeId) : null;

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <WorkSection
        title="📋 Kanban Board Example"
        subtitle="Interaktivní Kanban board s drag & drop"
        variant="compact"
      >
        <Alert severity="info" sx={{ mb: 3 }}>
          Tato stránka demonstruje Kanban board s pokročilým drag & drop pomocí dnd-kit knihovny.
          Můžete přesouvat úkoly mezi sloupci a měnit jejich pořadí.
        </Alert>

        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mb: 3 }}>
          <Button
            startIcon={<AddIcon />}
            variant="contained"
            onClick={() => setAddDialogOpen(true)}
          >
            Přidat úkol
          </Button>
          
          <Typography variant="body2" color="text.secondary">
            Celkem {tasks.length} úkolů
          </Typography>
        </Box>
      </WorkSection>

      {/* Kanban Board */}
      <Box sx={{ 
        flex: 1, 
        overflow: 'auto',
        pb: 2,
      }}>
        <DndContext
          sensors={sensors}
          collisionDetection={closestCorners}
          onDragStart={handleDragStart}
          onDragOver={handleDragOver}
          onDragEnd={handleDragEnd}
        >
          <Box sx={{ 
            display: 'flex', 
            gap: 3, 
            minHeight: 500,
            pb: 2,
          }}>
            <SortableContext items={columns.map(col => col.id)}>
              {columns.map((column) => (
                <KanbanColumn
                  key={column.id}
                  column={column}
                  tasks={tasksByColumn[column.id] || []}
                />
              ))}
            </SortableContext>
          </Box>

          <DragOverlay>
            {activeTask ? (
              <TaskCard task={activeTask} isDragging />
            ) : null}
          </DragOverlay>
        </DndContext>
      </Box>

      {/* Add Task Dialog */}
      <Dialog 
        open={addDialogOpen} 
        onClose={() => setAddDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Přidat nový úkol</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            <TextField
              label="Název úkolu"
              fullWidth
              variant="outlined"
            />
            <TextField
              label="Popis"
              fullWidth
              multiline
              rows={3}
              variant="outlined"
            />
            <FormControl fullWidth>
              <InputLabel>Přiřadit</InputLabel>
              <Select
                label="Přiřadit"
              >
                <MenuItem value="Jan Novák">Jan Novák</MenuItem>
                <MenuItem value="Marie Svobodová">Marie Svobodová</MenuItem>
                <MenuItem value="Petr Dvořák">Petr Dvořák</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Priorita</InputLabel>
              <Select
                label="Priorita"
                defaultValue="medium"
              >
                <MenuItem value="low">Nízká</MenuItem>
                <MenuItem value="medium">Střední</MenuItem>
                <MenuItem value="high">Vysoká</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddDialogOpen(false)}>
            Zrušit
          </Button>
          <Button 
            variant="contained" 
            onClick={() => setAddDialogOpen(false)}
          >
            Přidat úkol
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}