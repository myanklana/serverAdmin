import { Client } from '@stomp/stompjs';
import { useEffect, useState } from 'react';
import type { RealtimeMetric } from '../types/RealTimeMetric';

export type MetricStreamStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export function useMetricStream(
  token: string | null,
  onMetric: (metric: RealtimeMetric) => void,
) {
  const [status, setStatus] = useState<MetricStreamStatus>('disconnected');

  useEffect(() => {
    if (!token) {
      setStatus('disconnected');
      return;
    }

    setStatus('connecting');
    const client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws',
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
    });

    client.onConnect = () => {
      setStatus('connected');
      client.subscribe('/user/queue/metrics', frame => {
        try {
          onMetric(JSON.parse(frame.body) as RealtimeMetric);
        } catch (error) {
          console.error('Mensagem de métrica inválida.', error);
        }
      });
    };

    client.onStompError = frame => {
      setStatus('error');
      console.error('Erro STOMP:', frame.headers.message, frame.body);
    };
    client.onWebSocketError = event => {
      setStatus('error');
      console.error('Erro na conexão WebSocket.', event);
    };
    client.onWebSocketClose = () => setStatus('disconnected');

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [token, onMetric]);

  return status;
}
