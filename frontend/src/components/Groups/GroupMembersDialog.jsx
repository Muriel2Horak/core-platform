import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Alert,
  Box,
  Typography,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  IconButton,
  Autocomplete,
  TextField,
  CircularProgress,
  Chip
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Add as AddIcon,
  Person as PersonIcon
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

const GroupMembersDialog = ({ open, group, onClose, onMembersUpdated }) => {
  const [members, setMembers] = useState([]);
  const [availableUsers, setAvailableUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    if (group && open) {
      loadMembers();
      loadAvailableUsers();
    }
  }, [group, open]);

  const loadMembers = async () => {
    try {
      setLoading(true);
      const data = await apiService.getGroupMembers(group.name);
      setMembers(data || []);
    } catch (err) {
      console.error('Failed to load group members:', err);
      setError('Nepodařilo se načíst členy skupiny');
    } finally {
      setLoading(false);
    }
  };

  const loadAvailableUsers = async () => {
    try {
      setLoadingUsers(true);
      const users = await apiService.getUsers();
      setAvailableUsers(users || []);
    } catch (err) {
      console.error('Failed to load users:', err);
    } finally {
      setLoadingUsers(false);
    }
  };

  const handleAddMember = async () => {
    if (!selectedUser) return;

    try {
      setLoading(true);
      setError(null);

      await apiService.assignGroupToUser(selectedUser.id, {
        groupName: group.name
      });

      logger.userAction('GROUP_MEMBER_ADDED', { 
        group: group.name, 
        user: selectedUser.username 
      });

      setSuccess(`Uživatel ${selectedUser.username} byl přidán do skupiny`);
      setSelectedUser(null);
      await loadMembers();
      onMembersUpdated();
      
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      console.error('Failed to add member:', err);
      setError('Nepodařilo se přidat člena: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveMember = async (member) => {
    try {
      setLoading(true);
      setError(null);

      await apiService.removeGroupFromUser(member.id, group.name);

      logger.userAction('GROUP_MEMBER_REMOVED', { 
        group: group.name, 
        user: member.username 
      });

      setSuccess(`Uživatel ${member.username} byl odebrán ze skupiny`);
      await loadMembers();
      onMembersUpdated();
      
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      console.error('Failed to remove member:', err);
      setError('Nepodařilo se odebrat člena: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const getDisplayName = (user) => {
    if (user.firstName && user.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user.username;
  };

  const getInitials = (user) => {
    const name = getDisplayName(user);
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  // Filter out users who are already members
  const usersToAdd = availableUsers.filter(
    user => !members.some(member => member.id === user.id)
  );

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{ sx: { borderRadius: 3 } }}
    >
      <DialogTitle sx={{ fontWeight: 600, fontSize: '1.5rem' }}>
        👥 Členové skupiny: {group?.name}
      </DialogTitle>

      <DialogContent sx={{ pt: 2 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 2, borderRadius: 2 }} onClose={() => setSuccess(null)}>
            {success}
          </Alert>
        )}

        {/* Add Member */}
        <Box sx={{ mb: 3, p: 2, backgroundColor: 'action.hover', borderRadius: 2 }}>
          <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600 }}>
            Přidat člena
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Autocomplete
              fullWidth
              options={usersToAdd}
              getOptionLabel={(user) => `${getDisplayName(user)} (@${user.username})`}
              value={selectedUser}
              onChange={(e, newValue) => setSelectedUser(newValue)}
              loading={loadingUsers}
              disabled={loading}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Vyberte uživatele"
                  InputProps={{
                    ...params.InputProps,
                    endAdornment: (
                      <>
                        {loadingUsers ? <CircularProgress size={20} /> : null}
                        {params.InputProps.endAdornment}
                      </>
                    ),
                  }}
                  sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                />
              )}
            />
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleAddMember}
              disabled={!selectedUser || loading}
              sx={{ borderRadius: 2, minWidth: 120 }}
            >
              Přidat
            </Button>
          </Box>
        </Box>

        {/* Members List */}
        <Box>
          <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600, mb: 2 }}>
            Členové ({members.length})
          </Typography>

          {loading && !members.length ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : members.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <PersonIcon sx={{ fontSize: 60, color: 'text.secondary', mb: 1 }} />
              <Typography variant="body2" color="text.secondary">
                Skupina nemá žádné členy
              </Typography>
            </Box>
          ) : (
            <List sx={{ maxHeight: 400, overflow: 'auto' }}>
              {members.map((member) => (
                <ListItem
                  key={member.id}
                  sx={{
                    borderRadius: 2,
                    mb: 1,
                    backgroundColor: 'background.paper',
                    border: 1,
                    borderColor: 'divider'
                  }}
                  secondaryAction={
                    <IconButton
                      edge="end"
                      onClick={() => handleRemoveMember(member)}
                      disabled={loading}
                      color="error"
                    >
                      <DeleteIcon />
                    </IconButton>
                  }
                >
                  <ListItemAvatar>
                    <Avatar sx={{ backgroundColor: 'primary.main' }}>
                      {getInitials(member)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={getDisplayName(member)}
                    secondary={
                      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mt: 0.5 }}>
                        <Typography variant="caption" color="text.secondary">
                          @{member.username}
                        </Typography>
                        {member.email && (
                          <>
                            <Typography variant="caption" color="text.secondary">•</Typography>
                            <Typography variant="caption" color="text.secondary">
                              {member.email}
                            </Typography>
                          </>
                        )}
                      </Box>
                    }
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 3 }}>
        <Button onClick={onClose} sx={{ borderRadius: 2 }}>
          Zavřít
        </Button>
      </DialogActions>
    </Dialog>
  );
};

GroupMembersDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  group: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onMembersUpdated: PropTypes.func.isRequired
};

export default GroupMembersDialog;
