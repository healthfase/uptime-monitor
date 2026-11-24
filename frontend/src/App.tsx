import { useEffect, useState } from 'react';
import {
  Activity,
  Plus,
  Trash2,
  Search,
  RefreshCw,
  Globe,
  CheckCircle,
  XCircle,
  Clock,
  X,
} from 'lucide-react';
import SiteHistory from './components/SiteHistory';
import './App.css';

interface Site {
  id: number;
  name: string;
  url: string;
  online?: boolean;
  statusCode?: number;
  responseTimeMs?: number;
}

interface SiteStatistics {
  siteId: number;
  url: string;
  name: string;
  online: boolean;
  statusCode: number;
  responseTimeMs: number;
  totalChecks: number;
  successfulChecks: number;
  failedChecks: number;
  uptimePercentage: number;
  averageResponseTimeMs: number;
}

interface Dashboard {
  totalSites: number;
  onlineSites: number;
  offlineSites: number;
  overallUptime: number;
  sites: SiteStatistics[];
}

interface SitePageResponse {
  sites: Site[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

function App() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');

  const [newSiteName, setNewSiteName] = useState('');
  const [newSiteUrl, setNewSiteUrl] = useState('');
  const [adding, setAdding] = useState(false);

  const [selectedSite, setSelectedSite] = useState<Site | null>(null);

  const loadData = async () => {
    try {
      setError('');

      const [dashboardResponse, sitesResponse] = await Promise.all([
        fetch('/api/dashboard'),
        fetch('/api/sites?size=100'),
      ]);

      if (!dashboardResponse.ok || !sitesResponse.ok) {
        throw new Error('Не удалось загрузить данные сервера');
      }

      const dashboardData: Dashboard = await dashboardResponse.json();
      const sitesData: SitePageResponse = await sitesResponse.json();

      setDashboard(dashboardData);
      setSites(sitesData.sites);
    } catch (err) {
      setError(
          err instanceof Error ? err.message : 'Неизвестная ошибка'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();

    const interval = setInterval(loadData, 30000);

    return () => clearInterval(interval);
  }, []);

  const addSite = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!newSiteUrl.trim() || !newSiteName.trim()) {
      return;
    }

    try {
      setAdding(true);
      setError('');

      const response = await fetch('/api/sites', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: newSiteName,
          url: newSiteUrl,
        }),
      });

      if (!response.ok) {
        throw new Error('Не удалось добавить сайт');
      }

      setNewSiteName('');
      setNewSiteUrl('');

      await loadData();
    } catch (err) {
      setError(
          err instanceof Error ? err.message : 'Ошибка добавления сайта'
      );
    } finally {
      setAdding(false);
    }
  };

  const deleteSite = async (id: number) => {
    if (!window.confirm('Вы действительно хотите удалить этот сайт?')) {
      return;
    }

    try {
      const response = await fetch(`/api/sites/${id}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error('Не удалось удалить сайт');
      }

      if (selectedSite?.id === id) {
        setSelectedSite(null);
      }

      await loadData();
    } catch (err) {
      setError(
          err instanceof Error ? err.message : 'Ошибка удаления сайта'
      );
    }
  };

  const filteredSites = sites.filter((site) => {
    const matchesSearch =
        site.name.toLowerCase().includes(search.toLowerCase()) ||
        site.url.toLowerCase().includes(search.toLowerCase());

    const statistics = dashboard?.sites.find(
        (item) => item.siteId === site.id
    );

    const matchesStatus =
        statusFilter === 'all' ||
        (statusFilter === 'online' && statistics?.online === true) ||
        (statusFilter === 'offline' && statistics?.online === false);

    return matchesSearch && matchesStatus;
  });

  const getSiteStatus = (siteId: number) => {
    return dashboard?.sites.find((item) => item.siteId === siteId);
  };

  return (
      <main className="dashboard">
        <header className="header">
          <div className="header-title">
            <div className="logo">
              <Activity size={25} />
            </div>

            <div>
              <h1>Uptime Monitor</h1>
              <p>Мониторинг доступности сайтов</p>
            </div>
          </div>

          <button className="refresh-button" onClick={loadData}>
            <RefreshCw size={17} />
            Обновить
          </button>
        </header>

        {error && (
            <div className="error-message">
              {error}
            </div>
        )}

        {loading && !dashboard ? (
            <div className="loading">
              Загрузка данных...
            </div>
        ) : (
            <>
              <section className="stats-grid">
                <div className="stat-card">
                  <div className="stat-icon">
                    <Globe />
                  </div>
                  <p>Всего сайтов</p>
                  <h2>{dashboard?.totalSites ?? 0}</h2>
                </div>

                <div className="stat-card">
                  <div className="stat-icon online-icon">
                    <CheckCircle />
                  </div>
                  <p>Работают</p>
                  <h2>{dashboard?.onlineSites ?? 0}</h2>
                </div>

                <div className="stat-card">
                  <div className="stat-icon offline-icon">
                    <XCircle />
                  </div>
                  <p>Недоступны</p>
                  <h2>{dashboard?.offlineSites ?? 0}</h2>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">
                    <Clock />
                  </div>
                  <p>Общий uptime</p>
                  <h2>
                    {(dashboard?.overallUptime ?? 0).toFixed(2)}%
                  </h2>
                </div>
              </section>

              <section className="panel">
                <div className="panel-header">
                  <div>
                    <h2>Мои сайты</h2>
                    <p>Управление отслеживаемыми ресурсами</p>
                  </div>
                </div>

                <form className="add-site-form" onSubmit={addSite}>
                  <input
                      type="text"
                      placeholder="Название сайта"
                      value={newSiteName}
                      onChange={(e) => setNewSiteName(e.target.value)}
                      required
                  />

                  <input
                      type="url"
                      placeholder="https://example.com"
                      value={newSiteUrl}
                      onChange={(e) => setNewSiteUrl(e.target.value)}
                      required
                  />

                  <button type="submit" disabled={adding}>
                    <Plus size={17} />
                    {adding ? 'Добавление...' : 'Добавить'}
                  </button>
                </form>

                <div className="filters">
                  <div className="search-box">
                    <Search size={17} />
                    <input
                        type="text"
                        placeholder="Поиск сайта..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                  </div>

                  <select
                      value={statusFilter}
                      onChange={(e) => setStatusFilter(e.target.value)}
                  >
                    <option value="all">Все сайты</option>
                    <option value="online">Работают</option>
                    <option value="offline">Недоступны</option>
                  </select>
                </div>

                <div className="table-wrapper">
                  <table>
                    <thead>
                    <tr>
                      <th>Сайт</th>
                      <th>Статус</th>
                      <th>HTTP</th>
                      <th>Время ответа</th>
                      <th>Uptime</th>
                      <th>Действия</th>
                    </tr>
                    </thead>

                    <tbody>
                    {filteredSites.map((site) => {
                      const statistics = getSiteStatus(site.id);

                      return (
                          <tr
                              key={site.id}
                              className="site-row"
                              onClick={() => setSelectedSite(site)}
                          >
                            <td>
                              <div className="site-info">
                                <strong className="clickable-site">
                                  {site.name}
                                </strong>
                                <span>{site.url}</span>
                              </div>
                            </td>

                            <td>
                              {statistics?.online ? (
                                  <span className="status online">
                              <CheckCircle size={14} />
                              Online
                            </span>
                              ) : statistics ? (
                                  <span className="status offline">
                              <XCircle size={14} />
                              Offline
                            </span>
                              ) : (
                                  <span className="status unknown">
                              Не проверен
                            </span>
                              )}
                            </td>

                            <td>
                              {statistics?.statusCode ?? '—'}
                            </td>

                            <td>
                              {statistics
                                  ? `${statistics.responseTimeMs} ms`
                                  : '—'}
                            </td>

                            <td>
                              {statistics
                                  ? `${statistics.uptimePercentage.toFixed(2)}%`
                                  : '—'}
                            </td>

                            <td>
                              <button
                                  className="delete-button"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    deleteSite(site.id);
                                  }}
                                  title="Удалить сайт"
                              >
                                <Trash2 size={17} />
                              </button>
                            </td>
                          </tr>
                      );
                    })}

                    {filteredSites.length === 0 && (
                        <tr>
                          <td colSpan={6} className="empty-state">
                            Сайты не найдены
                          </td>
                        </tr>
                    )}
                    </tbody>
                  </table>
                </div>
              </section>

              {selectedSite && (
                  <section className="history-section">
                    <div className="history-section-header">
                      <h2>История проверок</h2>

                      <button
                          className="close-history"
                          onClick={() => setSelectedSite(null)}
                          title="Закрыть историю"
                      >
                        <X size={18} />
                      </button>
                    </div>

                    <SiteHistory
                        siteId={selectedSite.id}
                        siteName={selectedSite.name}
                    />
                  </section>
              )}
            </>
        )}
      </main>
  );
}

export default App;
