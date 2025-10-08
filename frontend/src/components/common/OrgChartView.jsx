import { useState } from 'react';
import PropTypes from 'prop-types';
import {
  Box,
  Paper,
  Typography,
  TextField,
  InputAdornment,
  CircularProgress,
  Avatar,
  Card,
  CardContent,
  Chip
} from '@mui/material';
import {
  Search as SearchIcon,
  AccountTree as OrgChartIcon,
  ExpandMore as ExpandMoreIcon,
  ChevronRight as ChevronRightIcon
} from '@mui/icons-material';

const OrgChartView = ({ 
  users, 
  onUserClick, 
  getDisplayName, 
  getInitials, 
  buildOrgHierarchy,
  loading 
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedUsers, setExpandedUsers] = useState(new Set());

  const toggleUser = (username) => {
    setExpandedUsers(prev => {
      const next = new Set(prev);
      if (next.has(username)) {
        next.delete(username);
      } else {
        next.add(username);
      }
      return next;
    });
  };

  const filteredHierarchy = () => {
    const hierarchy = buildOrgHierarchy();
    
    if (!searchQuery.trim()) {
      return hierarchy;
    }
    
    const query = searchQuery.toLowerCase();
    const filterNode = (node) => {
      const nameMatch = getDisplayName(node).toLowerCase().includes(query) ||
                       node.username?.toLowerCase().includes(query) ||
                       node.email?.toLowerCase().includes(query);
      
      const filteredChildren = (node.children || [])
        .map(filterNode)
        .filter(Boolean);
      
      if (nameMatch || filteredChildren.length > 0) {
        return {
          ...node,
          children: filteredChildren
        };
      }
      
      return null;
    };
    
    return hierarchy.map(filterNode).filter(Boolean);
  };

  const renderOrgNode = (node, level = 0) => {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedUsers.has(node.username);

    return (
      <Box key={node.username} sx={{ ml: level * 4 }}>
        <Card 
          sx={{ 
            mb: 1, 
            cursor: 'pointer',
            borderLeft: 3,
            borderLeftColor: level === 0 ? 'primary.main' : 
                            level === 1 ? 'secondary.main' : 'info.main',
            '&:hover': {
              backgroundColor: 'action.hover'
            }
          }}
        >
          <CardContent 
            sx={{ 
              p: 2, 
              '&:last-child': { pb: 2 },
              display: 'flex',
              alignItems: 'center',
              gap: 2
            }}
            onClick={() => onUserClick(node)}
          >
            {hasChildren && (
              <Box
                onClick={(e) => {
                  e.stopPropagation();
                  toggleUser(node.username);
                }}
                sx={{ 
                  display: 'flex',
                  alignItems: 'center',
                  cursor: 'pointer',
                  p: 0.5,
                  borderRadius: 1,
                  '&:hover': {
                    backgroundColor: 'action.selected'
                  }
                }}
              >
                {isExpanded ? <ExpandMoreIcon /> : <ChevronRightIcon />}
              </Box>
            )}
            
            {!hasChildren && <Box sx={{ width: 32 }} />}
            
            <Avatar
              sx={{
                width: 40,
                height: 40,
                backgroundColor: level === 0 ? 'primary.main' : 
                                level === 1 ? 'secondary.main' : 'info.main',
                fontSize: '0.9rem'
              }}
            >
              {getInitials(node)}
            </Avatar>
            
            <Box sx={{ flex: 1 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {getDisplayName(node)}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                @{node.username}
                {node.email && ` • ${node.email}`}
              </Typography>
            </Box>

            <Box sx={{ display: 'flex', gap: 0.5 }}>
              {node.isFederated && (
                <Chip
                  label="AD"
                  size="small"
                  color="info"
                  sx={{ borderRadius: 1 }}
                />
              )}
              {hasChildren && (
                <Chip
                  label={`${node.children.length} ${node.children.length === 1 ? 'podřízený' : 'podřízení'}`}
                  size="small"
                  variant="outlined"
                  sx={{ borderRadius: 1 }}
                />
              )}
            </Box>
          </CardContent>
        </Card>

        {hasChildren && isExpanded && (
          <Box sx={{ mt: 1 }}>
            {node.children.map(child => renderOrgNode(child, level + 1))}
          </Box>
        )}
      </Box>
    );
  };

  if (loading) {
    return (
      <Paper elevation={2} sx={{ borderRadius: 3, p: 4 }}>
        <Box display="flex" justifyContent="center">
          <CircularProgress />
        </Box>
      </Paper>
    );
  }

  const hierarchy = filteredHierarchy();

  return (
    <Paper elevation={2} sx={{ borderRadius: 3, overflow: 'hidden' }}>
      <Box sx={{ p: 3, borderBottom: 1, borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <OrgChartIcon color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            Organizační struktura
          </Typography>
        </Box>
        
        <TextField
          fullWidth
          placeholder="Vyhledat v org. struktuře..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            )
          }}
          sx={{
            '& .MuiOutlinedInput-root': {
              borderRadius: 2
            }
          }}
        />
      </Box>

      <Box sx={{ p: 3, maxHeight: '600px', overflowY: 'auto' }}>
        {hierarchy.length === 0 ? (
          <Box textAlign="center" py={4}>
            <OrgChartIcon sx={{ fontSize: 60, color: 'text.secondary', mb: 2 }} />
            <Typography variant="body1" color="text.secondary">
              {searchQuery ? 'Žádní uživatelé nenalezeni' : 'Žádná organizační struktura'}
            </Typography>
          </Box>
        ) : (
          hierarchy.map(node => renderOrgNode(node, 0))
        )}
      </Box>
    </Paper>
  );
};

OrgChartView.propTypes = {
  users: PropTypes.array.isRequired,
  onUserClick: PropTypes.func.isRequired,
  getDisplayName: PropTypes.func.isRequired,
  getInitials: PropTypes.func.isRequired,
  buildOrgHierarchy: PropTypes.func.isRequired,
  loading: PropTypes.bool
};

export default OrgChartView;
