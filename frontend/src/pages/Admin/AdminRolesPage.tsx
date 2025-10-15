import { Container, Box } from '@mui/material';
import Roles from '../../components/Roles.jsx';
import { useAuth } from '../../components/AuthProvider.jsx';
import { AiHelpWidget } from '../../components/AiHelpWidget';

export const AdminRolesPage = () => {
  const { user } = useAuth();
  const routeId = 'admin.roles.list';

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <AiHelpWidget routeId={routeId} />
      </Box>
      <Roles user={user} />
    </Container>
  );
};

export default AdminRolesPage;
