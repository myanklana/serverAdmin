import type { HistoricalMetric } from '../services/api';

type Series = { label: string; unit: string; color: string; value: (metric: HistoricalMetric) => number };

export function MetricChart({ title, metrics, series, maximum }: {
  title: string;
  metrics: HistoricalMetric[];
  series: Series[];
  maximum?: number;
}) {
  const width = 760;
  const height = 230;
  const plot = { left: 48, right: 14, top: 18, bottom: 34 };
  const values = metrics.flatMap(metric => series.map(item => item.value(metric))).filter(Number.isFinite);
  const ceiling = maximum ?? niceMaximum(Math.max(...values, 1));
  const x = (index: number) => plot.left + (index / Math.max(metrics.length - 1, 1)) * (width - plot.left - plot.right);
  const y = (value: number) => plot.top + (1 - Math.min(Math.max(value, 0), ceiling) / ceiling) * (height - plot.top - plot.bottom);
  const ticks = [0, .25, .5, .75, 1];

  return <article className="chart-card">
    <div className="chart-heading">
      <h3>{title}</h3>
      <div className="chart-legend">{series.map(item => <span key={item.label}><i style={{ background: item.color }} />{item.label}</span>)}</div>
    </div>
    {metrics.length < 2 ? <p className="empty-chart">Ainda não há dados suficientes para desenhar este gráfico.</p> :
      <svg className="metric-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label={`${title}, ${metrics.length} amostras`}>
        <title>{title}</title>
        {ticks.map(tick => {
          const tickY = y(ceiling * tick);
          return <g key={tick}>
            <line className="chart-grid" x1={plot.left} x2={width - plot.right} y1={tickY} y2={tickY} />
            <text className="chart-axis" x={plot.left - 8} y={tickY + 4} textAnchor="end">{formatAxis(ceiling * tick, series[0].unit)}</text>
          </g>;
        })}
        {series.map(item => <polyline key={item.label} fill="none" stroke={item.color} strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round"
          points={metrics.map((metric, index) => `${x(index)},${y(item.value(metric))}`).join(' ')} />)}
        <text className="chart-axis" x={plot.left} y={height - 8}>{formatTime(metrics[0].collectedAt)}</text>
        <text className="chart-axis" x={width - plot.right} y={height - 8} textAnchor="end">{formatTime(metrics[metrics.length - 1].collectedAt)}</text>
      </svg>}
  </article>;
}

function niceMaximum(value: number) {
  const magnitude = 10 ** Math.floor(Math.log10(value));
  return Math.ceil(value / magnitude) * magnitude;
}

function formatAxis(value: number, unit: string) {
  if (unit === '%') return `${Math.round(value)}%`;
  if (value >= 1024 ** 3) return `${(value / 1024 ** 3).toFixed(1)} GB/s`;
  if (value >= 1024 ** 2) return `${(value / 1024 ** 2).toFixed(1)} MB/s`;
  if (value >= 1024) return `${(value / 1024).toFixed(0)} KB/s`;
  return `${Math.round(value)} B/s`;
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}
