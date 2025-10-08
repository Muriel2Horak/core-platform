/**
 * 🎯 Příklady použití Permission System
 */

import { usePermissions } from '../hooks/usePermissions';
import { Button, IconButton } from '@mui/material';
import { Delete as DeleteIcon, Add as AddIcon } from '@mui/icons-material';

// ===== PŘÍKLAD 1: Podmíněné zobrazení tlačítek =====
function UsersList() {
  const { canCreate, canDelete, canUpdate, loading } = usePermissions();

  if (loading) return <div>Načítání...</div>;

  return (
    <div>
      <h1>Uživatelé</h1>
      
      {/* Tlačítko "Přidat" se zobrazí pouze pokud má permission users:create */}
      {canCreate('users') && (
        <Button startIcon={<AddIcon />} onClick={handleCreate}>
          Přidat uživatele
        </Button>
      )}

      <DataTable
        data={users}
        columns={[
          { field: 'name', label: 'Jméno' },
          { field: 'email', label: 'Email' },
          {
            field: 'actions',
            label: 'Akce',
            render: (value, user) => (
              <>
                {/* Ikona "Upravit" se zobrazí pouze pokud má permission users:update */}
                {canUpdate('users') && (
                  <IconButton onClick={() => handleEdit(user)}>
                    <EditIcon />
                  </IconButton>
                )}
                
                {/* Ikona "Smazat" se zobrazí pouze pokud má permission users:delete */}
                {canDelete('users') && (
                  <IconButton onClick={() => handleDelete(user)}>
                    <DeleteIcon />
                  </IconButton>
                )}
              </>
            ),
          },
        ]}
      />
    </div>
  );
}

// ===== PŘÍKLAD 2: Dynamické menu z permissions =====
import { Link } from 'react-router-dom';
import { List, ListItem, ListItemIcon, ListItemText } from '@mui/material';
import * as Icons from '@mui/icons-material';

function Navigation() {
  const { getMenuItems, loading } = usePermissions();

  if (loading) return <div>Načítání menu...</div>;

  const menuItems = getMenuItems();

  return (
    <List>
      {menuItems.map((item) => {
        const IconComponent = Icons[item.icon] || Icons.Dashboard;
        
        return (
          <ListItem key={item.id} button component={Link} to={item.path}>
            <ListItemIcon>
              <IconComponent />
            </ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItem>
        );
      })}
    </List>
  );
}

// ===== PŘÍKLAD 3: Podmíněné zobrazení celé stránky =====
function TenantsPage() {
  const { hasMenu, canRead, loading } = usePermissions();

  if (loading) return <div>Načítání...</div>;

  // Redirect pokud nemá přístup k menu
  if (!hasMenu('tenants')) {
    return <Navigate to="/dashboard" />;
  }

  // Nebo pokud nemá permission číst tenanty
  if (!canRead('tenants', 'all')) {
    return <Alert severity="error">Nemáte oprávnění k zobrazení tenantů.</Alert>;
  }

  return <div>Tenant management...</div>;
}

// ===== PŘÍKLAD 4: Feature flags =====
function DashboardPage() {
  const { hasFeature } = usePermissions();

  return (
    <div>
      <h1>Dashboard</h1>
      
      {/* Grafana widget se zobrazí pouze pokud má feature */}
      {hasFeature('grafana_admin') && (
        <GrafanaAdminPanel />
      )}
      
      {hasFeature('grafana_editor') && (
        <GrafanaEditorPanel />
      )}
      
      {hasFeature('grafana_viewer') && (
        <GrafanaViewerPanel />
      )}
      
      {/* Export tlačítko pouze pokud má feature */}
      {hasFeature('export_data') && (
        <Button onClick={handleExport}>Export dat</Button>
      )}
    </div>
  );
}

// ===== PŘÍKLAD 5: Data scope aware filtering =====
function UsersDataTable() {
  const { getDataScope, loading } = usePermissions();
  const [users, setUsers] = useState([]);

  useEffect(() => {
    const fetchUsers = async () => {
      const dataScope = getDataScope();
      
      // Backend automaticky filtruje podle data scope
      // ale můžeme zobrazit info uživateli
      const response = await apiService.getUsers();
      setUsers(response.data);
    };

    if (!loading) {
      fetchUsers();
    }
  }, [loading, getDataScope]);

  const dataScope = getDataScope();

  return (
    <div>
      <Alert severity="info">
        {dataScope === 'all_tenants' && 'Zobrazuji uživatele ze všech tenantů'}
        {dataScope === 'own_tenant' && 'Zobrazuji uživatele z vašeho tenantu'}
        {dataScope === 'own_data' && 'Zobrazuji pouze váš profil'}
      </Alert>
      
      <DataTable data={users} />
    </div>
  );
}

// ===== PŘÍKLAD 6: Granular permission check =====
function AdvancedUserForm({ userId }) {
  const { can } = usePermissions();

  return (
    <form>
      <TextField name="name" label="Jméno" />
      <TextField name="email" label="Email" />
      
      {/* Tenant selector pouze pro admins s all scope */}
      {can('users:create:all') && (
        <Select name="tenantKey" label="Tenant">
          {tenants.map(t => <MenuItem value={t.key}>{t.name}</MenuItem>)}
        </Select>
      )}
      
      {/* Role assignment pouze pokud má permission */}
      {can('roles:assign:all') || can('roles:assign:tenant') && (
        <MultiSelect name="roles" label="Role">
          {roles.map(r => <MenuItem value={r.name}>{r.name}</MenuItem>)}
        </MultiSelect>
      )}
      
      <Button type="submit">
        {userId ? 'Uložit' : 'Vytvořit'}
      </Button>
    </form>
  );
}

export {
  UsersList,
  Navigation,
  TenantsPage,
  DashboardPage,
  UsersDataTable,
  AdvancedUserForm,
};
