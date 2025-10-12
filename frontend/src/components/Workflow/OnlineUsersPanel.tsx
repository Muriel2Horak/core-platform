/**
 * W6: Online Users Panel (Collaboration)
 */

import {
  Box,
  Avatar,
  Tooltip,
  Typography,
} from '@mui/material';
import { People as PeopleIcon, Circle as OnlineIcon } from '@mui/icons-material';

interface CollaborationUser {
  userId: string;
  username: string;
}

interface OnlineUsersPanelProps {
  users: CollaborationUser[];
  connected: boolean;
}

const stringToColor = (str: string) => {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  const hue = hash % 360;
  return `hsl(${hue}, 70%, 60%)`;
};

export const OnlineUsersPanel = ({ users, connected }: OnlineUsersPanelProps) => {
  if (!connected || users.length === 0) {
    return null;
  }

  return (
    <Box 
      sx={{ 
        position: 'absolute', 
        top: 16, 
        right: 16, 
        zIndex: 1000,
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        bgcolor: 'background.paper',
        borderRadius: 2,
        px: 2,
        py: 1,
        boxShadow: 3,
      }}
    >
      <PeopleIcon color="primary" fontSize="small" />
      <Typography variant="body2" color="text.secondary">
        {users.length} online
      </Typography>
      
      <Box display="flex" gap={0.5}>
        {users.map((user) => (
          <Tooltip key={user.userId} title={user.username}>
            <Avatar 
              sx={{ 
                width: 32, 
                height: 32, 
                bgcolor: stringToColor(user.userId),
                fontSize: '0.875rem',
              }}
            >
              {user.username.charAt(0).toUpperCase()}
            </Avatar>
          </Tooltip>
        ))}
      </Box>

      <OnlineIcon color="success" sx={{ fontSize: 12, ml: 1 }} />
    </Box>
  );
};

export default OnlineUsersPanel;
