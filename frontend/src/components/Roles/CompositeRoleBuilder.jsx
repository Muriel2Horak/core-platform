import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Alert,
  CircularProgress,
  Typography,
  Chip,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Autocomplete,
  TextField,
  Divider,
} from '@mui/material';
import {
  Close as CloseIcon,
  Add as AddIcon,
  Remove as RemoveIcon,
  AccountTree as AccountTreeIcon,
  ArrowDownward as ArrowDownwardIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

/**
 * 🌳 Composite Role Builder
 * Vizuální editor pro hierarchii composite rolí
 */
export const CompositeRoleBuilder = ({ open, role, onClose, onSuccess }) => {
  const [allRoles, setAllRoles] = useState([]);
  const [currentComposites, setCurrentComposites] = useState([]);
  const [selectedRoleToAdd, setSelectedRoleToAdd] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [loadingComposites, setLoadingComposites] = useState(false);

  useEffect(() => {
    if (open && role) {
      loadData();
    }
  }, [open, role]);

  const loadData = async () => {
    try {
      setLoadingComposites(true);
      setError(null);

      // Load all roles
      const rolesData = await apiService.getRoles();
      setAllRoles(rolesData || []);

      // Load current composites
      const compositesData = await apiService.getRoleComposites(role.name);
      setCurrentComposites(compositesData || []);

      logger.info('Composite role data loaded', { 
        role: role.name, 
        composites: compositesData?.length 
      });

    } catch (err) {
      logger.error('Failed to load composite role data', { error: err.message });
      setError('Nepodařilo se načíst data hierarchie');
    } finally {
      setLoadingComposites(false);
    }
  };

  const handleAddComposite = async () => {
    if (!selectedRoleToAdd) return;

    try {
      setLoading(true);
      setError(null);

      logger.userAction('COMPOSITE_ROLE_ADD_ATTEMPT', { 
        parent: role.name, 
        child: selectedRoleToAdd.name 
      });

      await apiService.addCompositeRole(role.name, {
        childRoleName: selectedRoleToAdd.name
      });

      logger.userAction('COMPOSITE_ROLE_ADDED', { 
        parent: role.name, 
        child: selectedRoleToAdd.name 
      });

      // Reload composites
      await loadData();
      setSelectedRoleToAdd(null);

    } catch (err) {
      logger.error('Failed to add composite role', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se přidat child roli');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveComposite = async (childRole) => {
    try {
      setLoading(true);
      setError(null);

      logger.userAction('COMPOSITE_ROLE_REMOVE_ATTEMPT', { 
        parent: role.name, 
        child: childRole.name 
      });

      await apiService.removeCompositeRole(role.name, childRole.name);

      logger.userAction('COMPOSITE_ROLE_REMOVED', { 
        parent: role.name, 
        child: childRole.name 
      });

      // Reload composites
      await loadData();

    } catch (err) {
      logger.error('Failed to remove composite role', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se odebrat child roli');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setSelectedRoleToAdd(null);
    setError(null);
    onClose();
  };

  const handleSave = () => {
    onSuccess && onSuccess();
    handleClose();
  };

  // Filter out roles that are already composites or the role itself
  const availableRoles = allRoles.filter(r => 
    r.name !== role?.name && 
    !currentComposites.some(c => c.name === r.name)
  );

  if (!role) return null;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <AccountTreeIcon />
          <Typography variant="h6">Hierarchie role: {role.name}</Typography>
        </Box>
      </DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {!role.composite && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Tato role není nastavena jako composite role. Pro přidání child rolí ji nejprve upravte.
          </Alert>
        )}

        {/* Current hierarchy visualization */}
        <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: 'background.default' }}>
          <Typography variant="subtitle2" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <AccountTreeIcon fontSize="small" />
            Aktuální hierarchie
          </Typography>
          <Divider sx={{ my: 1 }} />

          {loadingComposites ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <Box sx={{ pl: 2 }}>
              {/* Parent role */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <Chip 
                  label={role.name} 
                  color="primary" 
                  size="small"
                  icon={<AccountTreeIcon />}
                />
                <Typography variant="caption" color="text.secondary">
                  (Parent role)
                </Typography>
              </Box>

              {/* Arrow */}
              {currentComposites.length > 0 && (
                <Box sx={{ display: 'flex', alignItems: 'center', pl: 2, py: 0.5 }}>
                  <ArrowDownwardIcon fontSize="small" color="action" />
                  <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                    obsahuje
                  </Typography>
                </Box>
              )}

              {/* Child roles */}
              {currentComposites.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ pl: 2, py: 1 }}>
                  Žádné child role
                </Typography>
              ) : (
                <List dense sx={{ pl: 2 }}>
                  {currentComposites.map((composite) => (
                    <ListItem 
                      key={composite.name}
                      sx={{ 
                        bgcolor: 'background.paper', 
                        borderRadius: 1,
                        mb: 0.5,
                        border: 1,
                        borderColor: 'divider',
                      }}
                    >
                      <ListItemText
                        primary={composite.name}
                        secondary={composite.description}
                      />
                      <ListItemSecondaryAction>
                        <IconButton
                          edge="end"
                          size="small"
                          onClick={() => handleRemoveComposite(composite)}
                          disabled={loading}
                          color="error"
                        >
                          <RemoveIcon />
                        </IconButton>
                      </ListItemSecondaryAction>
                    </ListItem>
                  ))}
                </List>
              )}
            </Box>
          )}
        </Paper>

        {/* Add new composite role */}
        {role.composite && (
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Přidat child roli
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
              <Autocomplete
                options={availableRoles}
                getOptionLabel={(option) => `${option.name}${option.description ? ` - ${option.description}` : ''}`}
                value={selectedRoleToAdd}
                onChange={(_, newValue) => setSelectedRoleToAdd(newValue)}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Vyberte roli"
                    placeholder="Začněte psát název role..."
                  />
                )}
                renderOption={(props, option) => (
                  <li {...props}>
                    <Box>
                      <Typography variant="body2">{option.name}</Typography>
                      {option.description && (
                        <Typography variant="caption" color="text.secondary">
                          {option.description}
                        </Typography>
                      )}
                    </Box>
                  </li>
                )}
                disabled={loading}
                fullWidth
                sx={{ flexGrow: 1 }}
              />
              <Button
                variant="contained"
                onClick={handleAddComposite}
                disabled={loading || !selectedRoleToAdd}
                startIcon={loading ? <CircularProgress size={20} /> : <AddIcon />}
                sx={{ minWidth: 120 }}
              >
                Přidat
              </Button>
            </Box>

            {availableRoles.length === 0 && (
              <Alert severity="info" sx={{ mt: 2 }}>
                Všechny dostupné role jsou již přidány do hierarchie.
              </Alert>
            )}
          </Box>
        )}

        {/* Info box */}
        <Alert severity="info" sx={{ mt: 3 }}>
          <Typography variant="body2">
            💡 <strong>Tip:</strong> Uživatelé s parent rolí automaticky získají všechna oprávnění všech child rolí.
            Například: CORE_ROLE_ADMIN obsahuje CORE_ROLE_USER_MANAGER, který obsahuje CORE_ROLE_USER.
          </Typography>
        </Alert>
      </DialogContent>

      <DialogActions>
        <Button 
          onClick={handleClose} 
          disabled={loading}
          startIcon={<CloseIcon />}
        >
          Zavřít
        </Button>
        <Button 
          onClick={handleSave}
          variant="contained"
          disabled={loading}
        >
          Hotovo
        </Button>
      </DialogActions>
    </Dialog>
  );
};

CompositeRoleBuilder.propTypes = {
  open: PropTypes.bool.isRequired,
  role: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
};
