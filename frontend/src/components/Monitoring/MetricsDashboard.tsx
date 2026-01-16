import { Box, Grid, Paper, Typography } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import ReactECharts from 'echarts-for-react';
import axios from 'axios';
import LiveMetricsWidget from './LiveMetricsWidget';

interface PrometheusSeries {
  metric: Record<string, string>;
  values: [number, string][];
}

interface PrometheusResponse {
  status: string;
  data?: {
    resultType: string;
    result: PrometheusSeries[];
  };
}

const defaultMinutes = 60;
const defaultStepSeconds = 60;

const metricConfigs = [
  { key: 'cpu_usage', title: 'CPU Usage (avg)' },
  { key: 'memory_used', title: 'Memory Used (bytes)' },
  { key: 'http_requests', title: 'HTTP Requests / s' },
];

const toChartSeries = (series: PrometheusSeries[]) => {
  if (!series.length) {
    return [];
  }

  return series.map((item) => ({
    name: item.metric?.instance || item.metric?.job || 'series',
    type: 'line',
    smooth: true,
    data: item.values.map(([timestamp, value]) => [timestamp * 1000, Number(value)]),
  }));
};

const MetricChart = ({ title, data }: { title: string; data: PrometheusResponse | null }) => {
  const series = useMemo(() => (data?.data?.result ? toChartSeries(data.data.result) : []), [data]);

  const option = {
    tooltip: {
      trigger: 'axis',
    },
    grid: { left: 40, right: 20, top: 40, bottom: 40 },
    xAxis: {
      type: 'time',
    },
    yAxis: {
      type: 'value',
      scale: true,
    },
    series,
  };

  return (
    <Paper sx={{ p: 2, height: 320 }}>
      <Typography variant="subtitle1" mb={2}>
        {title}
      </Typography>
      {series.length ? (
        <ReactECharts option={option} style={{ height: 240 }} />
      ) : (
        <Typography variant="body2" color="text.secondary">
          No data available.
        </Typography>
      )}
    </Paper>
  );
};

export const MetricsDashboard = () => {
  const [metrics, setMetrics] = useState<Record<string, PrometheusResponse | null>>({});

  useEffect(() => {
    let isMounted = true;

    const fetchMetric = async (metricKey: string) => {
      const response = await axios.get(`/api/monitoring/metrics/${metricKey}`, {
        params: {
          minutes: defaultMinutes,
          stepSeconds: defaultStepSeconds,
        },
      });
      return response.data as PrometheusResponse;
    };

    const load = async () => {
      try {
        const results = await Promise.all(
          metricConfigs.map(async (metric) => ({
            key: metric.key,
            data: await fetchMetric(metric.key),
          }))
        );

        if (!isMounted) {
          return;
        }

        const next: Record<string, PrometheusResponse> = {};
        results.forEach((item) => {
          next[item.key] = item.data;
        });
        setMetrics(next);
      } catch (error) {
        console.error('Failed to load metrics dashboard', error);
      }
    };

    load();
    const interval = setInterval(load, 60000);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  return (
    <Box display="flex" flexDirection="column" gap={3}>
      <LiveMetricsWidget />
      <Grid container spacing={2}>
        {metricConfigs.map((metric) => (
          <Grid item xs={12} md={4} key={metric.key}>
            <MetricChart title={metric.title} data={metrics[metric.key] || null} />
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default MetricsDashboard;
