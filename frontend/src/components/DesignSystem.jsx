// 🎨 Design System komponenty pro vývojáře
import React from 'react';
import PropTypes from 'prop-types';
import {
  Box,
  Typography,
  Grid,
  Card,
  CardContent,
  Button,
  TextField,
  IconButton,
  Tooltip,
  Skeleton,
  CircularProgress,
  Paper,
} from '@mui/material';

// ✅ Odstraněno: designTokens, theme proměnné - nepoužívané

// 🎯 Form Field Component s prop validací
const DSFormField = ({ label, error, helperText, required, ...props }) => (
  <TextField
    fullWidth
    label={label}
    error={error}
    helperText={helperText}
    required={required}
    variant="outlined"
    sx={{
      '& .MuiOutlinedInput-root': {
        borderRadius: 2
      }
    }}
    {...props}
  />
);

DSFormField.propTypes = {
  label: PropTypes.string,
  error: PropTypes.bool,
  helperText: PropTypes.string,
  required: PropTypes.bool,
};

// 🏗️ Card Component s prop validací
const DSCard = ({ title, children, actions }) => (
  <Card sx={{ borderRadius: 3, boxShadow: 2 }}>
    <CardContent>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6" fontWeight={600}>
          {title}
        </Typography>
        {actions && <Box>{actions}</Box>}
      </Box>
      {children}
    </CardContent>
  </Card>
);

DSCard.propTypes = {
  title: PropTypes.string,
  children: PropTypes.node,
  actions: PropTypes.node,
};

// 📦 Container Component s prop validací
const DSContainer = ({ children, maxWidth = 'lg' }) => (
  <Box maxWidth={maxWidth} mx="auto" px={3}>
    {children}
  </Box>
);

DSContainer.propTypes = {
  children: PropTypes.node,
  maxWidth: PropTypes.string,
};

// 📄 Page Header Component s prop validací  
const DSPageHeader = ({ title, subtitle, breadcrumbs, actions }) => (
  <Box mb={4}>
    {breadcrumbs && (
      <Box mb={1}>
        {breadcrumbs.map((crumb, index) => (
          <React.Fragment key={index}>
            <Typography variant="body2" color="text.secondary" component="span">
              {crumb}
            </Typography>
            {index < breadcrumbs.length - 1 && (
              <Typography variant="body2" color="text.secondary" component="span" mx={1}>
                /
              </Typography>
            )}
          </React.Fragment>
        ))}
      </Box>
    )}
    
    <Box display="flex" justifyContent="space-between" alignItems="flex-start">
      <Box>
        <Typography variant="h4" fontWeight={700} gutterBottom>
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="body1" color="text.secondary">
            {subtitle}
          </Typography>
        )}
      </Box>
      {actions && <Box>{actions}</Box>}
    </Box>
  </Box>
);

DSPageHeader.propTypes = {
  title: PropTypes.string,
  subtitle: PropTypes.string,
  breadcrumbs: PropTypes.arrayOf(PropTypes.string),
  actions: PropTypes.node,
};

// 🦴 Skeleton Loader s prop validací
const DSSkeleton = ({ variant = 'rectangular', width = '100%', height = 20, count = 1 }) => (
  <Box>
    {Array.from({ length: count }).map((_, index) => (
      <Skeleton
        key={index}
        variant={variant}
        width={width}
        height={height}
        sx={{ mb: 1, borderRadius: 1 }}
      />
    ))}
  </Box>
);

DSSkeleton.propTypes = {
  variant: PropTypes.string,
  width: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  height: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  count: PropTypes.number,
};

// 🔄 Loading Component s prop validací
const DSLoading = ({ size = 'medium', message }) => (
  <Box display="flex" flexDirection="column" alignItems="center" py={4}>
    <CircularProgress size={size === 'small' ? 24 : size === 'large' ? 48 : 32} />
    {message && (
      <Typography variant="body2" color="text.secondary" mt={2}>
        {message}
      </Typography>
    )}
  </Box>
);

DSLoading.propTypes = {
  size: PropTypes.string,
  message: PropTypes.string,
};

// 🔘 Button Group s prop validací
const DSButtonPrimary = ({ children }) => (
  <Button variant="contained" sx={{ borderRadius: 2 }}>
    {children}
  </Button>
);

DSButtonPrimary.propTypes = {
  children: PropTypes.node,
};

const DSButtonSecondary = ({ children }) => (
  <Button variant="outlined" sx={{ borderRadius: 2 }}>
    {children}
  </Button>
);

DSButtonSecondary.propTypes = {
  children: PropTypes.node,
};

const DSButtonText = ({ children }) => (
  <Button variant="text" sx={{ borderRadius: 2 }}>
    {children}
  </Button>
);

DSButtonText.propTypes = {
  children: PropTypes.node,
};

const DSButtonDanger = ({ children }) => (
  <Button variant="contained" color="error" sx={{ borderRadius: 2 }}>
    {children}
  </Button>
);

DSButtonDanger.propTypes = {
  children: PropTypes.node,
};

// 📊 Data Table Component s prop validací - odstraněno columns, data, loading
const DSDataTable = () => (
  <Paper sx={{ borderRadius: 2, overflow: 'hidden' }}>
    <Box p={2}>
      <Typography variant="h6">Data Table</Typography>
      <Typography variant="body2" color="text.secondary">
        Implementujte pomocí TanStack Table pro pokročilé funkce
      </Typography>
    </Box>
  </Paper>
);

// 🎯 Icon Button s prop validací
const DSIconButton = ({ children, ariaLabel, tooltip, sx }) => {
  const button = (
    <IconButton aria-label={ariaLabel} sx={{ ...sx }}>
      {children}
    </IconButton>
  );

  return tooltip ? (
    <Tooltip title={tooltip}>
      {button}
    </Tooltip>
  ) : button;
};

DSIconButton.propTypes = {
  children: PropTypes.node,
  ariaLabel: PropTypes.string,
  tooltip: PropTypes.string,
  sx: PropTypes.object,
};

// 📐 Layout Grid s prop validací
const DSLayoutGrid = ({ children, columns = 12 }) => (
  <Grid container spacing={3} columns={columns}>
    {children}
  </Grid>
);

DSLayoutGrid.propTypes = {
  children: PropTypes.node,
  columns: PropTypes.number,
};

// 🚨 Empty State Component s prop validací
const DSEmptyState = ({ icon, title, description, action, illustration }) => (
  <Box textAlign="center" py={6}>
    {illustration || (
      <Box mb={2}>
        {icon}
      </Box>
    )}
    <Typography variant="h6" gutterBottom>
      {title}
    </Typography>
    <Typography variant="body2" color="text.secondary" mb={3}>
      {description}
    </Typography>
    {action}
  </Box>
);

DSEmptyState.propTypes = {
  icon: PropTypes.node,
  title: PropTypes.string,
  description: PropTypes.string,
  action: PropTypes.node,
  illustration: PropTypes.node,
};

export default {
  DSFormField,
  DSCard,
  DSContainer,
  DSPageHeader,
  DSSkeleton,
  DSLoading,
  DSButtonPrimary,
  DSButtonSecondary,
  DSButtonText,
  DSButtonDanger,
  DSDataTable,
  DSIconButton,
  DSLayoutGrid,
  DSEmptyState
};