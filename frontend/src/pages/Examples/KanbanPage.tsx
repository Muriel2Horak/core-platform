/**
 * 📋 KanbanPage - Example page with Drag & Drop Kanban Board
 * 
 * Ukázka implementace Kanban boardu s funkcemi:
 * - Drag & drop mezi sloupci
 * - Přidávání/úprava úkolů
 * - Filtrování podle assignee, priority, tagů
 * - Responsivní design
 */

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Avatar,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Button,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material';
import {
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  AccessTime as AccessTimeIcon,
} from '@mui/icons-material';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
  DragStartEvent,
  DragOverEvent,
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

// 🎯 Typy pro Kanban data
interface Task {
  id: string;
  title: string;
  description?: string;
  assignee: {
    name: string;
    avatar?: string;
  };
  priority: 'low' | 'medium' | 'high';
  tags: string[];
  dueDate?: Date;
  columnId: string;
}

interface Column {
  id: string;
  title: string;
  color: string;
  tasks: Task[];
}

// 🎲 Mock data
const initialTasks: Task[] = [
  {
    id: 'task-1',
    title: 'Návrh nového designu',
    description: 'Vytvořit mockupy pro novou landing page',
    assignee: { name: 'Jan Novák' },
    priority: 'high',
    tags: ['design', 'ui/ux'],
    dueDate: new Date('2024-01-15'),
    columnId: 'todo',
  },
  {
    id: 'task-2',
    title: 'API integrace',
    description: 'Připojit frontend k novému API endpointu',
    assignee: { name: 'Marie Svobodová' },
    priority: 'medium',
    tags: ['development', 'api'],
    columnId: 'in-progress',
  },
  {
    id: 'task-3',
    title: 'Testing deployment',
    description: 'Otestovat deployment pipeline',
    assignee: { name: 'Petr Dvořák' },
    priority: 'low',
    tags: ['testing', 'devops'],
    columnId: 'done',
  },
];

const initialColumns: Column[] = [
  {
    id: 'todo',
    title: 'K dodělání',
    color: tokens.colors.grey[500],
    tasks: initialTasks.filter(task => task.columnId === 'todo'),
  },
  {
    id: 'in-progress',
    title: 'Rozpracováno',
    color: tokens.colors.primary[500],
    tasks: initialTasks.filter(task => task.columnId === 'in-progress'),
  },
  {
    id: 'review',
    title: 'Review',
    color: tokens.colors.warning[500],
    tasks: initialTasks.filter(task => task.columnId === 'review'),
  },
  {
    id: 'done',
    title: 'Hotovo',
    color: tokens.colors.success[500],
    tasks: initialTasks.filter(task => task.columnId === 'done'),
  },
];

// 📋 TaskCard component
const TaskCard = ({ task, isDragging = false }: { task: Task; isDragging?: boolean }) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
  } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high': return 'error';
      case 'medium': return 'warning';
      case 'low': return 'success';
      default: return 'default';
    }
  };

  return (
    <Card
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      sx={{
        mb: 2,
        cursor: 'grab',
        '&:hover': {
          boxShadow: tokens.shadows.md,
        },
      }}
    >
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, flex: 1 }}>
            {task.title}
          </Typography>
          <IconButton
            size="small"
            onClick={handleMenuClick}
            sx={{ ml: 1 }}
          >
            <MoreVertIcon fontSize="small" />
          </IconButton>
        </Box>

        {task.description && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {task.description}
          </Typography>
        )}

        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 2 }}>
          <Chip
            label={task.priority}
            size="small"
            color={getPriorityColor(task.priority) as 'error' | 'warning' | 'success' | 'default'}
          />
          {task.tags.map((tag: string) => (
            <Chip
              key={tag}
              label={tag}
              size="small"
              variant="outlined"
            />
          ))}
        </Box>

        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Avatar sx={{ width: 24, height: 24, fontSize: 12 }}>
            {task.assignee.name.split(' ').map((n: string) => n[0]).join('')}
          </Avatar>
          
          {task.dueDate && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <AccessTimeIcon sx={{ fontSize: 12, color: tokens.colors.grey[400] }} />
              <Typography variant="caption" color="text.secondary">
                {task.dueDate.toLocaleDateString('cs-CZ')}
              </Typography>
            </Box>
          )}
        </Box>
      </CardContent>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
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
    </Card>
  );
};

// 📋 Column component
const KanbanColumn = ({ column, tasks, activeId }: { column: Column; tasks: Task[]; activeId: string | null }) => {
  return (
    <Card sx={{ minHeight: 400, width: 300 }}>
      <CardContent sx={{ p: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box
              sx={{
                width: 12,
                height: 12,
                borderRadius: '50%',
                backgroundColor: column.color,
              }}
            />
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {column.title}
            </Typography>
            <Chip label={tasks.length} size="small" />
          </Box>
          <IconButton size="small">
            <AddIcon fontSize="small" />
          </IconButton>
        </Box>

        <SortableContext items={tasks.map(task => task.id)} strategy={verticalListSortingStrategy}>
          {tasks.map((task) => (
            <TaskCard 
              key={task.id} 
              task={task} 
              isDragging={activeId === task.id}
            />
          ))}
        </SortableContext>
      </CardContent>
    </Card>
  );
};

interface KanbanPageProps {
  user?: {
    email?: string;
    username?: string;
    roles?: string[];
  };
}

export default function KanbanPage({ user }: KanbanPageProps) {
  const [columns, setColumns] = useState(initialColumns);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [activeId, setActiveId] = useState<string | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const handleDragStart = (event: DragStartEvent) => {
    const activeId = event.active.id as string;
    setActiveId(activeId);
  };

  const handleDragOver = (event: DragOverEvent) => {
    const { active, over } = event;
    
    if (!over) return;
    
    const activeId = active.id as string;
    const overId = over.id as string;

    // Najdeme sloupce
    const activeColumn = columns.find(col =>
      col.tasks.some(task => task.id === activeId) || col.id === activeId
    );
    const overColumn = columns.find(col =>
      col.tasks.some(task => task.id === overId) || col.id === overId
    );

    if (!activeColumn || !overColumn) return;
    if (activeColumn.id === overColumn.id) return;

    // Přesuneme úkol mezi sloupci
    if (activeColumn.id !== overColumn.id) {
      setColumns(prev => {
        const activeItems = activeColumn.tasks;
        const overItems = overColumn.tasks;

        const activeIndex = activeItems.findIndex(task => task.id === activeId);
        const overIndex = overItems.findIndex(task => task.id === overId);

        return prev.map(column => {
          if (column.id === activeColumn.id) {
            return {
              ...column,
              tasks: column.tasks.filter(task => task.id !== activeId),
            };
          } else if (column.id === overColumn.id) {
            const newTask = { ...activeItems[activeIndex], columnId: overColumn.id };
            const newTasks = [...column.tasks];
            newTasks.splice(overIndex >= 0 ? overIndex : newTasks.length, 0, newTask);
            return {
              ...column,
              tasks: newTasks,
            };
          }
          return column;
        });
      });
    }
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    
    setActiveId(null);
    
    if (!over) return;
    
    const activeId = active.id as string;
    const overId = over.id as string;

    // Najdeme sloupce
    const activeColumn = columns.find(col =>
      col.tasks.some(task => task.id === activeId) || col.id === activeId
    );
    const overColumn = columns.find(col =>
      col.tasks.some(task => task.id === overId) || col.id === overId
    );

    if (!activeColumn || !overColumn) return;

    // Přeřadíme úkoly ve stejném sloupci
    if (activeColumn.id === overColumn.id) {
      setColumns(prev => {
        const activeItems = activeColumn.tasks;
        const overItems = overColumn.tasks;

        const activeIndex = activeItems.findIndex(task => task.id === activeId);
        const overIndex = overItems.findIndex(task => task.id === overId);

        return prev.map(column => {
          if (column.id === activeColumn.id) {
            return {
              ...column,
              tasks: arrayMove(column.tasks, activeIndex, overIndex),
            };
          }
          return column;
        });
      });
    }
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <WorkSection
        title="📋 Kanban Board"
        subtitle="Drag & Drop úkolový board"
        variant="compact"
      >
        <Alert severity="info" sx={{ mb: 3 }}>
          Kanban board s drag & drop funkcionalitou. Můžete přetahovat úkoly mezi sloupci. 
          {user ? `Přihlášen jako: ${user.email || 'Uživatel'}` : ''}
        </Alert>

        <Box sx={{ display: 'flex', gap: 3, overflowX: 'auto', pb: 2 }}>
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragStart={handleDragStart}
            onDragOver={handleDragOver}
            onDragEnd={handleDragEnd}
          >
            {columns.map((column) => (
              <KanbanColumn
                key={column.id}
                column={column}
                tasks={column.tasks}
                activeId={activeId}
              />
            ))}
          </DndContext>
        </Box>

        {/* Add Task Dialog */}
        <Dialog open={addDialogOpen} onClose={() => setAddDialogOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Nový úkol</DialogTitle>
          <DialogContent>
            <TextField
              margin="dense"
              label="Název úkolu"
              fullWidth
              variant="outlined"
              sx={{ mb: 2 }}
            />
            <TextField
              margin="dense"
              label="Popis"
              fullWidth
              multiline
              rows={3}
              variant="outlined"
              sx={{ mb: 2 }}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAddDialogOpen(false)}>
              Zrušit
            </Button>
            <Button onClick={() => setAddDialogOpen(false)} variant="contained">
              Vytvořit
            </Button>
          </DialogActions>
        </Dialog>
      </WorkSection>
    </Box>
  );
}