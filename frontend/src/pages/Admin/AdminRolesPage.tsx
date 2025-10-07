import { Container, Typography, Box } from '@mui/material';
import { Security as SecurityIcon } from '@mui/icons-material';
import { GlassPaper } from '../../shared/ui';

export const AdminRolesPage = () => {
  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <SecurityIcon fontSize="large" color="primary" />
        <Box>
          <Typography variant="h4">Správa rolí</Typography>
          <Typography variant="body2" color="text.secondary">
            Správa rolí a oprávnění v systému
          </Typography>
        </Box>
      </Box>
      <GlassPaper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Role management - připraveno pro implementaci
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Zde bude rozhraní pro správu CORE_ROLE_ADMIN, CORE_ROLE_USER_MANAGER a dalších rolí.
        </Typography>
      </GlassPaper>
    </Container>
  );
};

export default AdminRolesPage;
