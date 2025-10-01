import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Divider,
  Link,
  Chip,
  IconButton
} from '@mui/material';
import { Search, BusinessCenter, ExitToApp, History, Clear } from '@mui/icons-material';

const TenantDiscoveryPage = () => {
  const [identifier, setIdentifier] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [availableTenants, setAvailableTenants] = useState([]);
  const [showAvailable, setShowAvailable] = useState(false);
  const [recentTenants, setRecentTenants] = useState([]);

  // 🍪 SMART MEMORY: Load recent tenants and last user
  useEffect(() => {
    loadRecentTenants();
    loadLastUser();
  }, []);

  /**
   * 🍪 LOAD RECENT TENANTS: Z localStorage
   */
  const loadRecentTenants = () => {
    try {
      const stored = localStorage.getItem('core-platform-recent-tenants');
      if (stored) {
        const recent = JSON.parse(stored);
        setRecentTenants(recent.slice(0, 3)); // Pouze posledních 3
      }
    } catch (e) {
      console.warn('Failed to load recent tenants:', e);
    }
  };

  /**
   * 👤 LOAD LAST USER: Předvyplnit posledního uživatele
   */
  const loadLastUser = () => {
    try {
      const lastUser = localStorage.getItem('core-platform-last-user');
      if (lastUser) {
        setIdentifier(lastUser);
      }
    } catch (e) {
      console.warn('Failed to load last user:', e);
    }
  };

  /**
   * 💾 SAVE RECENT TENANT: Uložit do localStorage
   */
  const saveRecentTenant = (tenantKey, tenantName) => {
    try {
      const recent = [...recentTenants];

      // Remove if already exists
      const existingIndex = recent.findIndex(t => t.tenantKey === tenantKey);
      if (existingIndex !== -1) {
        recent.splice(existingIndex, 1);
      }

      // Add to beginning
      recent.unshift({
        tenantKey,
        tenantName,
        lastUsed: new Date().toISOString()
      });

      // Keep only last 5
      const trimmed = recent.slice(0, 5);

      localStorage.setItem('core-platform-recent-tenants', JSON.stringify(trimmed));
      setRecentTenants(trimmed.slice(0, 3)); // Show only 3

    } catch (e) {
      console.warn('Failed to save recent tenant:', e);
    }
  };

  /**
   * 💾 SAVE LAST USER: Uložit posledního uživatele
   */
  const saveLastUser = (userIdentifier) => {
    try {
      localStorage.setItem('core-platform-last-user', userIdentifier);
    } catch (e) {
      console.warn('Failed to save last user:', e);
    }
  };

  const handleSearch = async () => {
    if (!identifier.trim()) {
      setError('Zadejte email nebo uživatelské jméno');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await fetch('/api/tenant-discovery/find-tenant', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          identifier: identifier.trim()
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      setResult(data);

      // 💾 Save last user for next time
      saveLastUser(identifier.trim());

      // Auto-redirect if single tenant found
      if (data.found && data.autoRedirect && data.redirectUrl) {
        // 💾 Save recent tenant before redirect
        saveRecentTenant(data.tenantKey, data.tenantName);

        setTimeout(() => {
          window.location.href = data.redirectUrl;
        }, 2000);
      }

    } catch (err) {
      console.error('Tenant discovery failed:', err);
      setError(`Chyba při hledání tenantu: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleShowAvailable = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/tenant-discovery/available-tenants');

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      setAvailableTenants(data.tenants || []);
      setShowAvailable(true);

    } catch (err) {
      console.error('Failed to get available tenants:', err);
      setError(`Chyba při načítání tenantů: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleTenantSelect = (tenantKey, tenantName, tenantUrl) => {
    // 💾 Save recent tenant before redirect
    saveRecentTenant(tenantKey, tenantName);
    window.location.href = tenantUrl;
  };

  const handleRecentTenantSelect = (tenant) => {
    const url = `https://${tenant.tenantKey}.core-platform.local/`;
    handleTenantSelect(tenant.tenantKey, tenant.tenantName, url);
  };

  const clearRecentTenants = () => {
    localStorage.removeItem('core-platform-recent-tenants');
    localStorage.removeItem('core-platform-last-user');
    setRecentTenants([]);
    setIdentifier('');
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'grey.50',
        p: 2
      }}
    >
      <Card sx={{ maxWidth: 500, width: '100%' }}>
        <CardContent sx={{ p: 4 }}>
          {/* Header */}
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <BusinessCenter sx={{ fontSize: 48, color: 'primary.main', mb: 2 }} />
            <Typography variant="h4" gutterBottom>
              Core Platform
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Najděte svého poskytovatele pro přihlášení
            </Typography>
          </Box>

          {/* 🍪 RECENT TENANTS: Quick access */}
          {recentTenants.length > 0 && !showAvailable && (
            <Box sx={{ mb: 3 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  <History sx={{ fontSize: 16, mr: 0.5, verticalAlign: 'middle' }} />
                  Nedávno použité
                </Typography>
                <IconButton size="small" onClick={clearRecentTenants} title="Vymazat historii">
                  <Clear fontSize="small" />
                </IconButton>
              </Box>

              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {recentTenants.map((tenant) => (
                  <Chip
                    key={tenant.tenantKey}
                    label={tenant.tenantName}
                    onClick={() => handleRecentTenantSelect(tenant)}
                    clickable
                    variant="outlined"
                    size="small"
                    sx={{
                      '&:hover': { bgcolor: 'primary.50' }
                    }}
                  />
                ))}
              </Box>

              <Divider sx={{ mt: 2, mb: 2 }} />
            </Box>
          )}

          {/* Search Form */}
          {!showAvailable && (
            <Box sx={{ mb: 3 }}>
              <TextField
                fullWidth
                label="Email nebo uživatelské jméno"
                placeholder="john@company.com, john.doe nebo Jan Novák"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                disabled={loading}
                sx={{ mb: 2 }}
                helperText="Zadejte email, uživatelské jméno nebo část jména"
              />

              <Button
                fullWidth
                variant="contained"
                onClick={handleSearch}
                disabled={loading || !identifier.trim()}
                startIcon={loading ? <CircularProgress size={20} /> : <Search />}
                sx={{ mb: 2 }}
              >
                {loading ? 'Hledám...' : 'Najít můj tenant'}
              </Button>

              <Button
                fullWidth
                variant="outlined"
                onClick={handleShowAvailable}
                disabled={loading}
              >
                Zobrazit všechny dostupné tenanty
              </Button>
            </Box>
          )}

          {/* Error Display */}
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {/* Search Results */}
          {result && (
            <Box sx={{ mb: 2 }}>
              {result.found ? (
                <>
                  {result.autoRedirect ? (
                    <Alert severity="success">
                      <Typography variant="subtitle2">
                        Tenant nalezen: {result.tenantName}
                      </Typography>
                      <Typography variant="body2">
                        Přesměrovávám na přihlašovací stránku...
                      </Typography>
                    </Alert>
                  ) : result.multiple ? (
                    <Box>
                      <Alert severity="info" sx={{ mb: 2 }}>
                        Nalezeno více tenantů. Vyberte si:
                      </Alert>
                      <List>
                        {result.tenants.map((tenant, index) => (
                          <React.Fragment key={tenant.tenantKey}>
                            <ListItem disablePadding>
                              <ListItemButton
                                onClick={() => handleTenantSelect(tenant.tenantKey, tenant.tenantName, tenant.url)}
                              >
                                <ListItemText
                                  primary={tenant.tenantName}
                                  secondary={tenant.tenantKey}
                                />
                                <ExitToApp color="action" />
                              </ListItemButton>
                            </ListItem>
                            {index < result.tenants.length - 1 && <Divider />}
                          </React.Fragment>
                        ))}
                      </List>
                    </Box>
                  ) : null}
                </>
              ) : (
                <Alert severity="warning">
                  <Typography variant="subtitle2">
                    Tenant nenalezen
                  </Typography>
                  <Typography variant="body2">
                    Pro &quot;{result.identifier}&quot; nebyl nalezen žádný tenant.
                  </Typography>
                </Alert>
              )}
            </Box>
          )}

          {/* Available Tenants List */}
          {showAvailable && (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">
                  Dostupné tenanty ({availableTenants.length})
                </Typography>
                <Button
                  size="small"
                  onClick={() => setShowAvailable(false)}
                >
                  Zpět na vyhledávání
                </Button>
              </Box>

              {availableTenants.length > 0 ? (
                <List>
                  {availableTenants.map((tenant, index) => (
                    <React.Fragment key={tenant.tenantKey}>
                      <ListItem disablePadding>
                        <ListItemButton
                          onClick={() => handleTenantSelect(tenant.tenantKey, tenant.tenantName, tenant.url)}
                        >
                          <ListItemText
                            primary={tenant.tenantName}
                            secondary={`${tenant.tenantKey}.core-platform.local`}
                          />
                          <ExitToApp color="action" />
                        </ListItemButton>
                      </ListItem>
                      {index < availableTenants.length - 1 && <Divider />}
                    </React.Fragment>
                  ))}
                </List>
              ) : (
                <Alert severity="info">
                  Žádné tenanty nejsou dostupné.
                </Alert>
              )}
            </Box>
          )}

          {/* Footer */}
          <Box sx={{ textAlign: 'center', mt: 4, pt: 2, borderTop: 1, borderColor: 'divider' }}>
            <Typography variant="caption" color="text.secondary">
              Problémy s přihlášením? Kontaktujte{' '}
              <Link href="mailto:support@core-platform.local">
                support@core-platform.local
              </Link>
            </Typography>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default TenantDiscoveryPage;