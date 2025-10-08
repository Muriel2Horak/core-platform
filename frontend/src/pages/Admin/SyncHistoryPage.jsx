import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  List,
  ListItem,
  ListItemText,
  Divider,
  Grid,
  alpha,
  Alert
} from '@mui/material';
import {
  PlayArrow as RunningIcon,
  CheckCircle as CompletedIcon,
  Error as ErrorIcon,
  History as HistoryIcon
} from '@mui/icons-material';
import { DataTable } from '../../components/common/DataTable';
import axios from 'axios';

/**
 * 📊 Historie synchronizací s filtrací a detailem
 * 
 * Zobrazuje kompletní historii všech synchronizačních operací z DB.
 * Podporuje filtry podle stavu (běžící, dokončené, chybné) a detail každé operace.
 */
export const SyncHistoryPage = () => {
  const [activeTab, setActiveTab] = useState('running');
  const [syncData, setSyncData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedSync, setSelectedSync] = useState(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [totalCount, setTotalCount] = useState(0);

  /**
   * 📊 Definice sloupců podle metamodelu
   */
  const columns = [
    {
      field: 'type',
      label: 'Typ',
      type: 'custom',
      render: (value) => {
        const typeLabels = {
          users: 'Uživatelé',
          roles: 'Role',
          groups: 'Skupiny',
          all: 'Vše'
        };
        return typeLabels[value] || value;
      }
    },
    {
      field: 'tenantKey',
      label: 'Tenant',
      type: 'text'
    },
    {
      field: 'status',
      label: 'Stav',
      type: 'status',
      statusColors: {
        RUNNING: '#1976d2',
        COMPLETED: '#2e7d32',
        FAILED: '#d32f2f',
        CANCELLED: '#ed6c02'
      }
    },
    {
      field: 'processedItems',
      label: 'Progress',
      type: 'progress',
      totalField: 'totalItems',
      sortable: false
    },
    {
      field: 'startTime',
      label: 'Zahájeno',
      type: 'datetime'
    },
    {
      field: 'endTime',
      label: 'Dokončeno',
      type: 'datetime'
    },
    {
      field: 'initiatedBy',
      label: 'Spustil',
      type: 'text'
    }
  ];

  /**
   * 🔄 Načtení dat z API
   */
  const fetchData = async () => {
    setLoading(true);
    try {
      const statusParam = activeTab === 'all' ? '' : activeTab.toUpperCase();
      const response = await axios.get('/api/admin/sync-history', {
        params: {
          status: statusParam || undefined,
          page,
          size: rowsPerPage,
          sortBy: 'startTime',
          sortDir: 'DESC'
        }
      });

      setSyncData(response.data.content || []);
      setTotalCount(response.data.totalElements || 0);
    } catch (error) {
      console.error('Failed to fetch sync history:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [activeTab, page, rowsPerPage]);

  /**
   * 🔍 Zobrazení detailu synchronizace
   */
  const handleRowClick = (row) => {
    setSelectedSync(row);
    setDetailOpen(true);
  };

  /**
   * 🗑️ Smazání záznamu
   */
  const handleDelete = async (id) => {
    if (!confirm('Opravdu chcete smazat tento záznam?')) return;

    try {
      await axios.delete(`/api/admin/sync-history/${id}`);
      fetchData();
    } catch (error) {
      console.error('Failed to delete sync:', error);
    }
  };

  /**
   * 📑 Tab konfigurace
   */
  const tabs = [
    { value: 'running', label: 'Běžící', icon: <RunningIcon />, color: '#1976d2' },
    { value: 'completed', label: 'Dokončené', icon: <CompletedIcon />, color: '#2e7d32' },
    { value: 'failed', label: 'Chybné', icon: <ErrorIcon />, color: '#d32f2f' },
    { value: 'all', label: 'Vše', icon: <HistoryIcon />, color: '#757575' }
  ];

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography
          variant="h4"
          gutterBottom
          sx={{
            fontWeight: 700,
            background: theme => `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.secondary.main} 100%)`,
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent'
          }}
        >
          📊 Historie Synchronizací
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Kompletní přehled všech synchronizačních operací z Keycloak
        </Typography>
      </Box>

      {/* Tabs pro filtraci */}
      <Paper
        elevation={0}
        sx={{
          mb: 3,
          background: theme => `linear-gradient(135deg, ${alpha(theme.palette.primary.main, 0.05)} 0%, ${alpha(theme.palette.secondary.main, 0.05)} 100%)`,
          border: '1px solid',
          borderColor: 'divider'
        }}
      >
        <Tabs
          value={activeTab}
          onChange={(e, newValue) => {
            setActiveTab(newValue);
            setPage(0);
          }}
          sx={{
            '& .MuiTab-root': {
              minHeight: 64,
              textTransform: 'none',
              fontSize: '1rem',
              fontWeight: 600
            }
          }}
        >
          {tabs.map(tab => (
            <Tab
              key={tab.value}
              value={tab.value}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  {React.cloneElement(tab.icon, { sx: { color: tab.color } })}
                  {tab.label}
                </Box>
              }
            />
          ))}
        </Tabs>
      </Paper>

      {/* DataTable */}
      <DataTable
        columns={columns}
        data={syncData}
        loading={loading}
        totalCount={totalCount}
        onRowClick={handleRowClick}
        onDelete={handleDelete}
        onRefresh={fetchData}
        onPageChange={(newPage, newRowsPerPage) => {
          setPage(newPage);
          setRowsPerPage(newRowsPerPage);
        }}
        defaultRowsPerPage={20}
      />

      {/* Detail Dialog */}
      <Dialog
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        maxWidth="md"
        fullWidth
      >
        {selectedSync && (
          <>
            <DialogTitle
              sx={{
                background: theme => `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.secondary.main} 100%)`,
                color: 'white',
                fontWeight: 700
              }}
            >
              Detail Synchronizace: {selectedSync.id}
            </DialogTitle>
            <DialogContent sx={{ mt: 2 }}>
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Typ</Typography>
                  <Typography variant="body1" fontWeight={600}>{selectedSync.type}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Tenant</Typography>
                  <Typography variant="body1" fontWeight={600}>{selectedSync.tenantKey}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Stav</Typography>
                  <Typography variant="body1" fontWeight={600}>{selectedSync.status}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Spustil</Typography>
                  <Typography variant="body1" fontWeight={600}>{selectedSync.initiatedBy}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Zahájeno</Typography>
                  <Typography variant="body1">
                    {selectedSync.startTime ? new Date(selectedSync.startTime).toLocaleString('cs-CZ') : '-'}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">Dokončeno</Typography>
                  <Typography variant="body1">
                    {selectedSync.endTime ? new Date(selectedSync.endTime).toLocaleString('cs-CZ') : '-'}
                  </Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">Progress</Typography>
                  <Typography variant="body1">
                    {selectedSync.processedItems || 0} / {selectedSync.totalItems || 0} položek
                    ({selectedSync.totalItems > 0 ? Math.round((selectedSync.processedItems / selectedSync.totalItems) * 100) : 0}%)
                  </Typography>
                </Grid>
              </Grid>

              {selectedSync.errors && selectedSync.errors.length > 0 && (
                <>
                  <Divider sx={{ my: 2 }} />
                  <Alert severity="error" sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" fontWeight={700}>
                      Chyby ({selectedSync.errors.length})
                    </Typography>
                  </Alert>
                  <List dense>
                    {selectedSync.errors.map((error, idx) => (
                      <ListItem key={idx} sx={{ px: 0 }}>
                        <ListItemText
                          primary={error}
                          primaryTypographyProps={{
                            fontSize: '0.875rem',
                            color: 'error.main'
                          }}
                        />
                      </ListItem>
                    ))}
                  </List>
                </>
              )}
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setDetailOpen(false)}>Zavřít</Button>
            </DialogActions>
          </>
        )}
      </Dialog>
    </Box>
  );
};
