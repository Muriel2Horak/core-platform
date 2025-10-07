import TenantManagement from '../../components/TenantManagement';

export const AdminTenantsPage = ({ user }: { user: any }) => {
  return <TenantManagement user={user} />;
};

export default AdminTenantsPage;
