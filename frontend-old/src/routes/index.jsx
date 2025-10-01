import { createBrowserRouter } from 'react-router-dom';
import App from '../App';
import Dashboard from '../views/dashboard/Dashboard';
import TenantDiscoveryPage from '../views/TenantDiscoveryPage';
import UserManagementPage from '../views/admin/UserManagementPage';
import TenantManagementPage from '../views/admin/TenantManagementPage';
import KeycloakTestPage from '../views/test/KeycloakTestPage';
import ProfilePage from '../views/profile/ProfilePage';

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,
        element: <Dashboard />
      },
      {
        path: 'tenant-discovery',
        element: <TenantDiscoveryPage />
      },
      {
        path: 'profile',
        element: <ProfilePage />
      },
      {
        path: 'admin-core',
        children: [
          {
            path: 'users',
            element: <UserManagementPage />
          },
          {
            path: 'tenants',
            element: <TenantManagementPage />
          }
        ]
      },
      {
        path: 'test',
        children: [
          {
            path: 'keycloak',
            element: <KeycloakTestPage />
          }
        ]
      }
    ]
  }
]);

export default router;