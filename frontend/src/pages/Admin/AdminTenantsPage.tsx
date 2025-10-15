import { Box } from '@mui/material';
import TenantManagement from '../../components/TenantManagement';
import { AiHelpWidget } from '../../components/AiHelpWidget';

export const AdminTenantsPage = ({ user }: { user: any }) => {
  const routeId = 'admin.tenants.list';
  
  return (
    <Box data-route-id={routeId}>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <AiHelpWidget routeId={routeId} />
      </Box>
      <TenantManagement user={user} />
    </Box>
  );
};

export default AdminTenantsPage;
