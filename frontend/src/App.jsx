import { useState } from 'react';
import CustomerList from './components/CustomerList';
import CustomerForm from './components/CustomerForm';
import BulkUpload   from './components/BulkUpload';

// ============================================================
//  App.jsx — final version with all pages wired in
//  Location: src/App.jsx  (replace your existing one)
// ============================================================

export default function App() {
  const [page, setPage]             = useState('list');
  const [selectedId, setSelectedId] = useState(null);

  const handleNavigate = (destination, id = null) => {
    setSelectedId(id);
    setPage(destination);
  };

  return (
    <div style={appStyles.container}>

      {/* ── Top Navigation Bar ─────────────────────────── */}
      <nav style={appStyles.nav}>
        <div style={appStyles.navBrand}>
          <span style={appStyles.navLogo}>CMS</span>
          <span style={appStyles.navTitle}>Customer Management</span>
        </div>
        <div style={appStyles.navLinks}>
          <button
            style={{ ...appStyles.navLink, ...(page === 'list'   ? appStyles.navLinkActive : {}) }}
            onClick={() => handleNavigate('list')}
          >
            All Customers
          </button>
          <button
            style={{ ...appStyles.navLink, ...(page === 'create' ? appStyles.navLinkActive : {}) }}
            onClick={() => handleNavigate('create')}
          >
            + New Customer
          </button>
          <button
            style={{ ...appStyles.navLink, ...(page === 'bulk'   ? appStyles.navLinkActive : {}) }}
            onClick={() => handleNavigate('bulk')}
          >
            ↑ Bulk Upload
          </button>
        </div>
      </nav>

      {/* ── Page Content ───────────────────────────────── */}
      <main style={appStyles.main}>

        {page === 'list' && (
          <CustomerList onNavigate={handleNavigate} />
        )}

        {page === 'create' && (
          <CustomerForm customerId={null} onNavigate={handleNavigate} />
        )}

        {page === 'edit' && (
          <CustomerForm customerId={selectedId} onNavigate={handleNavigate} />
        )}

        {page === 'view' && (
          // Re-use CustomerForm in read mode — or replace with a detail component later
          <CustomerForm customerId={selectedId} onNavigate={handleNavigate} />
        )}

        {page === 'bulk' && (
          <BulkUpload onNavigate={handleNavigate} />
        )}

      </main>
    </div>
  );
}

const appStyles = {
  container: {
    minHeight: '100vh',
    backgroundColor: '#f8fafc',
    fontFamily: "'DM Sans', 'Segoe UI', sans-serif",
  },
  nav: {
    background: '#fff',
    borderBottom: '1px solid #e2e8f0',
    padding: '0 32px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: '60px',
    position: 'sticky',
    top: 0,
    zIndex: 100,
    boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
  },
  navBrand: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  navLogo: {
    background: '#3b82f6',
    color: '#fff',
    padding: '4px 8px',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '700',
    letterSpacing: '0.05em',
  },
  navTitle: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#0f172a',
  },
  navLinks: {
    display: 'flex',
    gap: '4px',
  },
  navLink: {
    background: 'none',
    border: 'none',
    padding: '8px 16px',
    borderRadius: '8px',
    fontSize: '14px',
    color: '#475569',
    cursor: 'pointer',
    fontWeight: '500',
    transition: 'all 0.15s',
  },
  navLinkActive: {
    background: '#eff6ff',
    color: '#3b82f6',
  },
  main: {
    minHeight: 'calc(100vh - 60px)',
  },
};