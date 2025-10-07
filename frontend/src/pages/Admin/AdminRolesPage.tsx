import { Container } from '@mui/material';
import Roles from '../../components/Roles.jsx';
import { useAuth } from '../../components/AuthProvider.jsx';

export const AdminRolesPage = () => {
  const { user } = useAuth();

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Roles user={user} />
    </Container>
  );
};

export default AdminRolesPage;
