import React, { useState, useEffect } from 'react';
import {
  Box,
  TextField,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Typography,
  CircularProgress,
  Alert,
  Chip,
  InputAdornment,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';

interface Entity {
  name: string;
  entity: string;
  table: string;
  fields?: any[];
}

interface ModelTreeProps {
  onSelectEntity: (entity: Entity) => void;
  selectedEntity: Entity | null;
}

/**
 * S10-B: ModelTree component - Read-only viewer for metamodel entities
 * 
 * Features:
 * - Load all entities from /api/admin/studio/entities
 * - Search/filter entities by name
 * - Display entity count
 * - Click to select entity for detail view
 */
export function ModelTree({ onSelectEntity, selectedEntity }: ModelTreeProps) {
  const [entities, setEntities] = useState<Entity[]>([]);
  const [filteredEntities, setFilteredEntities] = useState<Entity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadEntities();
  }, []);

  useEffect(() => {
    // Filter entities by search term
    if (searchTerm) {
      const filtered = entities.filter((e) =>
        e.name.toLowerCase().includes(searchTerm.toLowerCase())
      );
      setFilteredEntities(filtered);
    } else {
      setFilteredEntities(entities);
    }
  }, [searchTerm, entities]);

  const loadEntities = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/admin/studio/entities');

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();

      if (data.status === 'success') {
        setEntities(data.entities || []);
        setFilteredEntities(data.entities || []);
      } else {
        throw new Error(data.message || 'Failed to load entities');
      }
    } catch (err: any) {
      console.error('Failed to load entities:', err);
      setError(err.message || 'Nepodařilo se načíst entity');
    } finally {
      setLoading(false);
    }
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress size={40} />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mt: 2 }}>
        {error}
      </Alert>
    );
  }

  return (
    <Box>
      {/* Search bar */}
      <TextField
        size="small"
        fullWidth
        placeholder="Hledat entitu..."
        value={searchTerm}
        onChange={handleSearchChange}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon />
            </InputAdornment>
          ),
        }}
        sx={{ mb: 2 }}
      />

      {/* Entity count */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="caption" color="text.secondary">
          {filteredEntities.length} entit
        </Typography>
        <Chip
          label={`${filteredEntities.length}/${entities.length}`}
          size="small"
          color="primary"
          variant="outlined"
        />
      </Box>

      {/* Entity list */}
      <List dense>
        {filteredEntities.map((entity) => (
          <ListItem key={entity.name} disablePadding>
            <ListItemButton
              selected={selectedEntity?.name === entity.name}
              onClick={() => onSelectEntity(entity)}
              sx={{
                borderRadius: 1,
                mb: 0.5,
              }}
            >
              <ListItemText
                primary={entity.name}
                secondary={`${entity.table} (${entity.fields?.length || 0} polí)`}
                primaryTypographyProps={{
                  fontWeight: 'medium',
                  fontSize: '0.875rem',
                }}
                secondaryTypographyProps={{
                  fontSize: '0.75rem',
                }}
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      {filteredEntities.length === 0 && (
        <Alert severity="info" sx={{ mt: 2 }}>
          Žádné entity nenalezeny
        </Alert>
      )}
    </Box>
  );
}

export default ModelTree;
