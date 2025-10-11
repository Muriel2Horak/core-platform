import { useState } from 'react';
import {
  Box,
  Container,
  Paper,
  Tabs,
  Tab,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
  Breadcrumbs,
  Link
} from '@mui/material';
import { ExplorerGrid } from './ExplorerGrid';
import { ChartGrid } from './ChartPanel';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`reporting-tabpanel-${index}`}
      aria-labelledby={`reporting-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

function a11yProps(index: number) {
  return {
    id: `reporting-tab-${index}`,
    'aria-controls': `reporting-tabpanel-${index}`,
  };
}

/**
 * ReportingPage - Main reporting interface
 * 
 * Features:
 * - Entity selector
 * - Three view modes: Table, Charts, Pivot (future)
 * - Drill-down breadcrumbs
 * - Full integration with ExplorerGrid and ChartPanel
 */
export function ReportingPage() {
  const [entity, setEntity] = useState('users_directory');
  const [activeTab, setActiveTab] = useState(0);
  const [filters, setFilters] = useState({});
  const [breadcrumbs, setBreadcrumbs] = useState<Array<{ entity: string; label: string; filters: any }>>([]);

  // Available entities (TODO: fetch from /api/reports/metadata)
  const availableEntities = [
    { value: 'users_directory', label: 'Users' },
    { value: 'tenants_registry', label: 'Tenants' },
    { value: 'keycloak_groups', label: 'Groups' }
  ];

  const handleEntityChange = (newEntity: string) => {
    setEntity(newEntity);
    setFilters({});
    setBreadcrumbs([]);
  };

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  const handleDrillDown = (drillDownData: { entity?: string; filters?: any; label?: string }) => {
    const targetEntity = drillDownData.entity || entity;
    const newFilters = { ...filters, ...drillDownData.filters };

    // Add to breadcrumbs
    setBreadcrumbs([
      ...breadcrumbs,
      {
        entity,
        label: drillDownData.label || `${entity} (filtered)`,
        filters: { ...filters }
      }
    ]);

    setEntity(targetEntity);
    setFilters(newFilters);
  };

  const handleBreadcrumbClick = (index: number) => {
    if (index === -1) {
      // Home
      setEntity(availableEntities[0].value);
      setFilters({});
      setBreadcrumbs([]);
    } else {
      const target = breadcrumbs[index];
      setEntity(target.entity);
      setFilters(target.filters);
      setBreadcrumbs(breadcrumbs.slice(0, index));
    }
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h4" component="h1">
            Reporting Explorer
          </Typography>

          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel id="entity-selector-label">Entity</InputLabel>
            <Select
              labelId="entity-selector-label"
              id="entity-selector"
              value={entity}
              label="Entity"
              onChange={(e) => handleEntityChange(e.target.value)}
            >
              {availableEntities.map((ent) => (
                <MenuItem key={ent.value} value={ent.value}>
                  {ent.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        {breadcrumbs.length > 0 && (
          <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 2 }}>
            <Link
              color="inherit"
              href="#"
              onClick={(e) => {
                e.preventDefault();
                handleBreadcrumbClick(-1);
              }}
            >
              Home
            </Link>
            {breadcrumbs.map((crumb, index) => (
              <Link
                key={index}
                color="inherit"
                href="#"
                onClick={(e) => {
                  e.preventDefault();
                  handleBreadcrumbClick(index);
                }}
              >
                {crumb.label}
              </Link>
            ))}
            <Typography color="text.primary">
              {availableEntities.find(e => e.value === entity)?.label || entity}
            </Typography>
          </Breadcrumbs>
        )}

        <Tabs value={activeTab} onChange={handleTabChange} aria-label="reporting views">
          <Tab label="Table View" {...a11yProps(0)} />
          <Tab label="Charts" {...a11yProps(1)} />
          <Tab label="Pivot Table" {...a11yProps(2)} disabled />
        </Tabs>
      </Paper>

      <TabPanel value={activeTab} index={0}>
        <ExplorerGrid 
          entity={entity} 
          initialFilters={filters}
          onDrillDown={handleDrillDown}
        />
      </TabPanel>

      <TabPanel value={activeTab} index={1}>
        <ChartGrid 
          entity={entity}
          charts={[
            { type: 'bar', xField: 'status', yField: 'count', title: 'By Status' },
            { type: 'pie', xField: 'tenant_id', yField: 'count', title: 'By Tenant' }
          ]}
        />
      </TabPanel>

      <TabPanel value={activeTab} index={2}>
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="text.secondary">
            Pivot Table View - Coming Soon
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            This feature will use FINOS Perspective for advanced pivot table analysis
          </Typography>
        </Box>
      </TabPanel>
    </Container>
  );
}

export default ReportingPage;
