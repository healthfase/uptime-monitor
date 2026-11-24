
import { useEffect, useState } from 'react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
} from 'recharts';
import { CheckCircle, XCircle, Clock } from 'lucide-react';

interface Check {
    id: number;
    online: boolean;
    statusCode: number;
    responseTimeMs: number;
    checkedAt: string;
    message: string;
}

interface SiteHistoryProps {
    siteId: number;
    siteName: string;
}

function SiteHistory({ siteId, siteName }: SiteHistoryProps) {
    const [history, setHistory] = useState<Check[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const loadHistory = async () => {
            try {
                setLoading(true);
                setError('');

                const response = await fetch(
                    `/api/sites/${siteId}/history?limit=50`
                );

                if (!response.ok) {
                    throw new Error('Не удалось загрузить историю проверок');
                }

                const data: Check[] = await response.json();

                setHistory(data);
            } catch (err) {
                setError(
                    err instanceof Error ? err.message : 'Неизвестная ошибка'
                );
            } finally {
                setLoading(false);
            }
        };

        loadHistory();
    }, [siteId]);

    const chronologicalHistory = [...history].reverse();

    const chartData = chronologicalHistory.map((check) => ({
        time: new Date(check.checkedAt).toLocaleTimeString('ru-RU', {
            hour: '2-digit',
            minute: '2-digit',
        }),
        responseTime: check.responseTimeMs,
    }));

    const onlineCount = history.filter((check) => check.online).length;
    const offlineCount = history.length - onlineCount;

    if (loading) {
        return (
            <div className="history-panel">
                <h3>История проверок: {siteName}</h3>
                <p>Загрузка истории...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="history-panel">
                <h3>История проверок: {siteName}</h3>
                <p className="history-error">{error}</p>
            </div>
        );
    }

    return (
        <div className="history-panel">
            <h3>История проверок: {siteName}</h3>

            <div className="history-summary">
                <div>
                    <CheckCircle size={17} className="history-online" />
                    Успешные: <strong>{onlineCount}</strong>
                </div>

                <div>
                    <XCircle size={17} className="history-offline" />
                    Неудачные: <strong>{offlineCount}</strong>
                </div>

                <div>
                    <Clock size={17} />
                    Всего: <strong>{history.length}</strong>
                </div>
            </div>

            <h4>Время ответа</h4>

            {chartData.length > 0 ? (
                <div className="chart">
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={chartData}>
                            <CartesianGrid strokeDasharray="3 3" />

                            <XAxis
                                dataKey="time"
                                tick={{ fontSize: 11 }}
                            />

                            <YAxis
                                tick={{ fontSize: 11 }}
                                unit=" ms"
                            />

                            <Tooltip
                                formatter={(value) => [
                                    `${value} ms`,
                                    'Время ответа',
                                ]}
                            />

                            <Line
                                type="monotone"
                                dataKey="responseTime"
                                stroke="#3269d8"
                                strokeWidth={2}
                                dot={false}
                                activeDot={{ r: 5 }}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </div>
            ) : (
                <p>История проверок пока пуста.</p>
            )}

            <div className="history-list">
                <h4>Последние проверки</h4>

                {history.slice(0, 10).map((check) => (
                    <div className="history-row" key={check.id}>
            <span>
              {new Date(check.checkedAt).toLocaleString('ru-RU')}
            </span>

                        <span
                            className={
                                check.online ? 'history-online' : 'history-offline'
                            }
                        >
              {check.online ? 'Online' : 'Offline'}
            </span>

                        <span>HTTP {check.statusCode || '—'}</span>

                        <span>{check.responseTimeMs} ms</span>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default SiteHistory;
