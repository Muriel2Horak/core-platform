import { useState, useEffect, useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Grid,
  CircularProgress,
  Alert
} from '@mui/material';
import axios from 'axios';

/**
 * ChartPanel - Interactive charts with drill-down capability
 * 
 * Features:
 * - Multiple chart types (bar, line, pie)
 * - Click-to-drill-down navigation
 * - Dynamic data loading from reporting API
 * - Responsive layout
 * 
 * @param {Object} props
 * @param {string} props.entity - Entity name
 * @param {string} props.type - Chart type ('bar' | 'line' | 'pie')
 * @param {string} props.xField - X-axis dimension field
 * @param {string} props.yField - Y-axis measure field (or count)
 * @param {Function} props.onDrillDown - Drill-down handler (value) => void
 */
export function ChartPanel({ 
  entity, 
  type = 'bar', 
  xField = 'status', 
  yField = 'count',
  onDrillDown 
}) {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [chartType, setChartType] = useState(type);

  // Fetch chart data
  useEffect(() => {
    async function fetchData() {
      setLoading(true);
      setError(null);

      try {
        const queryRequest = {
          entity,
          dimensions: [xField],
          measures: yField === 'count' ? [] : [{ field: yField, aggregation: 'sum' }],
          groupBy: [xField]
        };

        const response = await axios.post('/api/reports/query', queryRequest);
        
        // Transform data for ECharts
        const chartData = (response.data.data || []).map(row => ({
          name: row[xField] || 'Unknown',
          value: yField === 'count' ? 1 : (row[yField] || 0)
        }));

        // Aggregate by name
        const aggregated = chartData.reduce((acc, item) => {
          const existing = acc.find(a => a.name === item.name);
          if (existing) {
            existing.value += item.value;
          } else {
            acc.push({ ...item });
          }
          return acc;
        }, []);

        setData(aggregated);
      } catch (err) {
        console.error('Failed to fetch chart data:', err);
        setError(err.response?.data?.message || 'Failed to load chart data');
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, [entity, xField, yField]);

  // ECharts option
  const option = useMemo(() => {
    if (data.length === 0) return {};

    const baseOption = {
      title: {
        text: `${entity} by ${xField}`,
        left: 'center',
        top: 10
      },
      tooltip: {
        trigger: chartType === 'pie' ? 'item' : 'axis',
        formatter: chartType === 'pie' 
          ? '{b}: {c} ({d}%)'
          : '{b0}: {c0}'
      },
      legend: {
        orient: 'horizontal',
        bottom: 10,
        data: data.map(d => d.name)
      }
    };

    if (chartType === 'pie') {
      return {
        ...baseOption,
        series: [{
          name: yField,
          type: 'pie',
          radius: '55%',
          center: ['50%', '50%'],
          data: data.map(d => ({ name: d.name, value: d.value })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          label: {
            formatter: '{b}: {d}%'
          }
        }]
      };
    }

    // Bar or Line chart
    return {
      ...baseOption,
      xAxis: {
        type: 'category',
        data: data.map(d => d.name),
        axisLabel: {
          rotate: data.length > 5 ? 45 : 0,
          interval: 0
        }
      },
      yAxis: {
        type: 'value',
        name: yField === 'count' ? 'Count' : yField
      },
      series: [{
        name: yField,
        type: chartType,
        data: data.map(d => d.value),
        itemStyle: {
          color: '#1976d2'
        },
        smooth: chartType === 'line',
        areaStyle: chartType === 'line' ? {
          opacity: 0.3
        } : undefined
      }],
      grid: {
        left: '10%',
        right: '10%',
        bottom: '15%',
        top: '20%'
      }
    };
  }, [data, chartType, entity, xField, yField]);

  // Click handler for drill-down
  const onChartClick = (params) => {
    if (onDrillDown && params.name) {
      onDrillDown({
        [xField]: params.name,
        value: params.value
      });
    }
  };

  const onEvents = {
    click: onChartClick
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height={400}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error}
      </Alert>
    );
  }

  if (data.length === 0) {
    return (
      <Alert severity="info" sx={{ m: 2 }}>
        No data available for this chart
      </Alert>
    );
  }

  return (
    <Card>
      <CardHeader
        title={`${entity} Analytics`}
        action={
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Chart Type</InputLabel>
            <Select
              value={chartType}
              label="Chart Type"
              onChange={(e) => setChartType(e.target.value)}
            >
              <MenuItem value="bar">Bar</MenuItem>
              <MenuItem value="line">Line</MenuItem>
              <MenuItem value="pie">Pie</MenuItem>
            </Select>
          </FormControl>
        }
      />
      <CardContent>
        <ReactECharts
          option={option}
          onEvents={onEvents}
          style={{ height: '400px', width: '100%' }}
          opts={{ renderer: 'canvas' }}
        />
      </CardContent>
    </Card>
  );
}

/**
 * ChartGrid - Grid of multiple charts for dashboard view
 * 
 * @param {Object} props
 * @param {string} props.entity - Entity name
 * @param {Array} props.charts - Array of chart configurations
 */
export function ChartGrid({ entity, charts }) {
  const defaultCharts = charts || [
    { type: 'bar', xField: 'status', yField: 'count', title: 'By Status' },
    { type: 'pie', xField: 'department', yField: 'count', title: 'By Department' }
  ];

  return (
    <Grid container spacing={2}>
      {defaultCharts.map((chart, index) => (
        <Grid item xs={12} md={6} key={index}>
          <ChartPanel
            entity={entity}
            type={chart.type}
            xField={chart.xField}
            yField={chart.yField}
          />
        </Grid>
      ))}
    </Grid>
  );
}
