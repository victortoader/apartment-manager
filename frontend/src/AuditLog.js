import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

const LANGUAGES = [
  { code: 'en', label: 'EN' },
  { code: 'de', label: 'DE' },
  { code: 'it', label: 'IT' },
  { code: 'fr', label: 'FR' },
  { code: 'ro', label: 'RO' }
];

function LanguageSwitcher() {
  const { i18n } = useTranslation();
  return (
    <div className="lang-switcher">
      {LANGUAGES.map(l => (
        <button
          key={l.code}
          className={`lang-btn${i18n.language === l.code ? ' active' : ''}`}
          onClick={() => i18n.changeLanguage(l.code)}
        >
          {l.label}
        </button>
      ))}
    </div>
  );
}

function AuditLog() {
  const { t } = useTranslation();
  const { user, logout, authHeader } = useAuth();
  const [logs, setLogs] = useState([]);
  const [filter, setFilter] = useState('');
  const [allUsers, setAllUsers] = useState([]);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const fetchLogs = useCallback(async () => {
    const url = filter
      ? `${API}/api/audit?username=${encodeURIComponent(filter)}`
      : `${API}/api/audit`;
    const res = await fetch(url, { headers: authHeader() });
    if (res.ok) {
      setLogs(await res.json());
    }
  }, [filter, authHeader]);

  const fetchUsers = async () => {
    const res = await fetch(`${API}/api/users`, { headers: authHeader() });
    if (res.ok) {
      setAllUsers(await res.json());
    }
  };

  useEffect(() => {
    fetchLogs();
    fetchUsers();
  }, []);

  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(fetchLogs, 5000);
    return () => clearInterval(interval);
  }, [autoRefresh, fetchLogs]);

  const formatTimestamp = (ts) => {
    if (!ts) return '';
    try {
      const d = Array.isArray(ts)
        ? new Date(ts[0], ts[1] - 1, ts[2], ts[3], ts[4], ts[5])
        : new Date(ts);
      return d.toLocaleString();
    } catch {
      return String(ts);
    }
  };

  const roleBadge = (role) => {
    if (!role) return <span className="role-badge role-tenant">{t('auditLog.unknown')}</span>;
    const cls = role === 'OWNER' ? 'role-owner' : role === 'ADMIN' ? 'role-admin' : 'role-tenant';
    return <span className={`role-badge ${cls}`}>{role}</span>;
  };

  const actionBadge = (action) => {
    if (!action) return <span className="action-badge">{action}</span>;
    let cls = 'action-other';
    if (action.startsWith('LOGIN')) cls = 'action-login';
    else if (action.includes('CREATED')) cls = 'action-create';
    else if (action.includes('UPDATED')) cls = 'action-update';
    else if (action.includes('DELETED')) cls = 'action-delete';
    else if (action.includes('UPLOADED')) cls = 'action-upload';
    else if (action.includes('ASSIGNED')) cls = 'action-assign';
    return <span className={`action-badge ${cls}`}>{action.replace(/_/g, ' ')}</span>;
  };

  const uniqueUsernames = [...new Set(logs.map(l => l.username).filter(Boolean))];

  return (
    <div className="app">
      <header>
        <h1>{t('auditLog.title')}</h1>
        <div className="header-right">
          <span className="header-user">{user.username} ({user.role})</span>
          <LanguageSwitcher />
          <a href="/" className="btn-back">{t('header.apartments')}</a>
          <button className="btn-back" onClick={logout}>{t('logout')}</button>
        </div>
      </header>

      <div className="audit-controls">
        <div className="audit-filter">
          <label>{t('auditLog.filterByUser')}</label>
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">{t('auditLog.allUsers')}</option>
            {uniqueUsernames.map(u => (
              <option key={u} value={u}>{u}</option>
            ))}
          </select>
          {filter && (
            <button className="btn-back" onClick={() => setFilter('')} style={{ marginLeft: '8px' }}>
              {t('auditLog.clear')}
            </button>
          )}
        </div>
        <div className="audit-refresh">
          <label>
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            {' '}{t('auditLog.autoRefresh')}
          </label>
          <button className="btn-back" onClick={fetchLogs} style={{ marginLeft: '12px' }}>
            {t('auditLog.refreshNow')}
          </button>
        </div>
      </div>

      <div className="audit-table-wrapper">
        <table className="audit-table">
          <thead>
            <tr>
              <th>{t('auditLog.time')}</th>
              <th>{t('auditLog.user')}</th>
              <th>{t('auditLog.role')}</th>
              <th>{t('auditLog.action')}</th>
              <th>{t('auditLog.details')}</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 && (
              <tr>
                <td colSpan="5" className="audit-empty">{t('auditLog.noLogs')}</td>
              </tr>
            )}
            {logs.map(log => (
              <tr key={log.id}>
                <td className="audit-time">{formatTimestamp(log.timestamp)}</td>
                <td className="audit-username">{log.username}</td>
                <td>{roleBadge(log.role)}</td>
                <td>{actionBadge(log.action)}</td>
                <td className="audit-details">{log.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="audit-count">
        {logs.length === 1
          ? t('auditLog.logTotal', { count: logs.length })
          : t('auditLog.logsTotal', { count: logs.length })
        }
      </div>
    </div>
  );
}

export default AuditLog;
