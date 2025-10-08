import PropTypes from 'prop-types';
import {
  Box,
  Typography,
  Avatar,
  Card,
  CardContent,
  Divider,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Chip
} from '@mui/material';
import {
  ArrowUpward as ArrowUpwardIcon,
  ArrowDownward as ArrowDownwardIcon,
  Person as PersonIcon
} from '@mui/icons-material';

const UserOrgChart = ({ 
  user, 
  users,
  onUserClick, 
  getDisplayName, 
  getInitials, 
  getUserHierarchy 
}) => {
  const { ancestors, descendants } = getUserHierarchy(user.username);

  return (
    <Box sx={{ p: 3 }}>
      {/* Ancestors (Managers Above) */}
      {ancestors.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <ArrowUpwardIcon fontSize="small" color="primary" />
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              Nadřízení ({ancestors.length})
            </Typography>
          </Box>
          
          <Box sx={{ pl: 2 }}>
            {ancestors.map((ancestor, index) => (
              <Box key={ancestor.username}>
                <Card 
                  sx={{ 
                    mb: 1,
                    cursor: 'pointer',
                    ml: index * 2,
                    '&:hover': {
                      backgroundColor: 'action.hover'
                    }
                  }}
                  onClick={() => onUserClick(ancestor)}
                >
                  <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                      <Avatar
                        sx={{
                          width: 36,
                          height: 36,
                          backgroundColor: 'primary.main',
                          fontSize: '0.85rem'
                        }}
                      >
                        {getInitials(ancestor)}
                      </Avatar>
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="body1" sx={{ fontWeight: 600 }}>
                          {getDisplayName(ancestor)}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          @{ancestor.username}
                        </Typography>
                      </Box>
                      <Chip
                        label={`Level ${ancestors.length - index}`}
                        size="small"
                        color="primary"
                        sx={{ borderRadius: 1 }}
                      />
                    </Box>
                  </CardContent>
                </Card>
              </Box>
            ))}
          </Box>
        </Box>
      )}

      {/* Current User */}
      <Box sx={{ mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <PersonIcon fontSize="small" color="success" />
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Aktuální uživatel
          </Typography>
        </Box>
        
        <Card 
          sx={{ 
            borderLeft: 4,
            borderLeftColor: 'success.main'
          }}
        >
          <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Avatar
                sx={{
                  width: 48,
                  height: 48,
                  backgroundColor: 'success.main',
                  fontSize: '1rem',
                  fontWeight: 'bold'
                }}
              >
                {getInitials(user)}
              </Avatar>
              <Box sx={{ flex: 1 }}>
                <Typography variant="body1" sx={{ fontWeight: 600 }}>
                  {getDisplayName(user)}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  @{user.username}
                  {user.email && ` • ${user.email}`}
                </Typography>
              </Box>
            </Box>
          </CardContent>
        </Card>
      </Box>

      {/* Descendants (Direct Reports) */}
      {descendants.length > 0 ? (
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <ArrowDownwardIcon fontSize="small" color="secondary" />
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              Podřízení ({descendants.length})
            </Typography>
          </Box>
          
          <List sx={{ p: 0 }}>
            {descendants.map((descendant, index) => {
              // Calculate level based on manager chain
              let level = 1;
              let current = descendant;
              while (current.manager !== user.username) {
                const parent = users.find(u => u.username === current.manager);
                if (!parent || parent.username === user.username) break;
                level++;
                current = parent;
                if (level > 10) break; // Prevent infinite loop
              }

              return (
                <ListItem
                  key={descendant.username}
                  sx={{
                    mb: 1,
                    p: 0
                  }}
                >
                  <Card 
                    sx={{ 
                      width: '100%',
                      cursor: 'pointer',
                      ml: (level - 1) * 2,
                      '&:hover': {
                        backgroundColor: 'action.hover'
                      }
                    }}
                    onClick={() => onUserClick(descendant)}
                  >
                    <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar
                          sx={{
                            width: 36,
                            height: 36,
                            backgroundColor: 'secondary.main',
                            fontSize: '0.85rem'
                          }}
                        >
                          {getInitials(descendant)}
                        </Avatar>
                        <Box sx={{ flex: 1 }}>
                          <Typography variant="body1" sx={{ fontWeight: 600 }}>
                            {getDisplayName(descendant)}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            @{descendant.username}
                          </Typography>
                        </Box>
                        {level > 1 && (
                          <Chip
                            label={`Level ${level}`}
                            size="small"
                            color="secondary"
                            sx={{ borderRadius: 1 }}
                          />
                        )}
                      </Box>
                    </CardContent>
                  </Card>
                </ListItem>
              );
            })}
          </List>
        </Box>
      ) : (
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <ArrowDownwardIcon fontSize="small" color="text.secondary" />
            <Typography variant="subtitle1" sx={{ fontWeight: 600, color: 'text.secondary' }}>
              Podřízení
            </Typography>
          </Box>
          <Box 
            sx={{ 
              p: 3, 
              textAlign: 'center',
              backgroundColor: 'action.hover',
              borderRadius: 2
            }}
          >
            <Typography variant="body2" color="text.secondary">
              Tento uživatel nemá žádné podřízené
            </Typography>
          </Box>
        </Box>
      )}
    </Box>
  );
};

UserOrgChart.propTypes = {
  user: PropTypes.object.isRequired,
  users: PropTypes.array.isRequired,
  onUserClick: PropTypes.func.isRequired,
  getDisplayName: PropTypes.func.isRequired,
  getInitials: PropTypes.func.isRequired,
  getUserHierarchy: PropTypes.func.isRequired
};

export default UserOrgChart;
