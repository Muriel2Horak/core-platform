import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Chip,
  Divider
} from '@mui/material';
import {
  Group as GroupIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  People as PeopleIcon,
  Business as BusinessIcon
} from '@mui/icons-material';

const ViewGroupDialog = ({ open, group, onClose, onEdit, onDelete }) => {
  if (!group) return null;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{ sx: { borderRadius: 3 } }}
    >
      <DialogTitle sx={{ fontWeight: 600, fontSize: '1.5rem' }}>
        👁️ Detail skupiny
      </DialogTitle>

      <DialogContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
          <GroupIcon sx={{ fontSize: 48, color: 'primary.main' }} />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              {group.name}
            </Typography>
            {group.path && (
              <Typography variant="body2" color="text.secondary">
                {group.path}
              </Typography>
            )}
          </Box>
        </Box>

        <Divider sx={{ my: 2 }} />

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {group.tenantKey && (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Tenant
              </Typography>
              <Chip
                icon={<BusinessIcon fontSize="small" />}
                label={group.tenantKey}
                size="small"
                color="primary"
                variant="outlined"
                sx={{ borderRadius: 2 }}
              />
            </Box>
          )}

          <Box>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              Počet členů
            </Typography>
            <Chip
              icon={<PeopleIcon fontSize="small" />}
              label={group.memberCount || 0}
              size="small"
              color="info"
              sx={{ borderRadius: 2 }}
            />
          </Box>

          {group.attributes && Object.keys(group.attributes).length > 0 && (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Atributy
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {Object.entries(group.attributes).map(([key, value]) => (
                  <Chip
                    key={key}
                    label={`${key}: ${Array.isArray(value) ? value.join(', ') : value}`}
                    size="small"
                    variant="outlined"
                    sx={{ borderRadius: 2 }}
                  />
                ))}
              </Box>
            </Box>
          )}
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 3, gap: 1 }}>
        <Button onClick={onClose} sx={{ borderRadius: 2 }}>
          Zavřít
        </Button>
        <Box sx={{ flex: 1 }} />
        <Button
          startIcon={<EditIcon />}
          onClick={onEdit}
          variant="outlined"
          sx={{ borderRadius: 2 }}
        >
          Upravit
        </Button>
        <Button
          startIcon={<DeleteIcon />}
          onClick={onDelete}
          variant="outlined"
          color="error"
          sx={{ borderRadius: 2 }}
        >
          Smazat
        </Button>
      </DialogActions>
    </Dialog>
  );
};

ViewGroupDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  group: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired
};

export default ViewGroupDialog;
