import { useState, useEffect, useCallback } from 'react';
import { customerApi } from '../api/axios';

// ============================================================
//  CustomerList.jsx
//  Location: src/components/CustomerList.jsx
//  Shows all customers in a paginated, searchable table.
//  Links to create, edit, and view individual customers.
// ============================================================

export default function CustomerList({ onNavigate }) {
  const [customers, setCustomers]     = useState([]);
  const [search, setSearch]           = useState('');
  const [page, setPage]               = useState(0);
  const [totalPages, setTotalPages]   = useState(0);
  const [totalItems, setTotalItems]   = useState(0);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState('');
  const [deleteId, setDeleteId]       = useState(null);   // ID being confirmed for delete
  const [deleting, setDeleting]       = useState(false);
  const [successMsg, setSuccessMsg]   = useState('');

  const PAGE_SIZE = 10;

  // -------------------------------------------------------
  // Load customers from backend
  // -------------------------------------------------------
  const loadCustomers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await customerApi.getAll(search, page, PAGE_SIZE);
      setCustomers(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalItems(res.data.totalElements);
    } catch (err) {
      setError('Failed to load customers. Is the backend running?');
    } finally {
      setLoading(false);
    }
  }, [search, page]);

  // Reload when search or page changes
  useEffect(() => {
    const delay = setTimeout(loadCustomers, 300); // debounce search
    return () => clearTimeout(delay);
  }, [loadCustomers]);

  // -------------------------------------------------------
  // Search — reset to page 0 when search term changes
  // -------------------------------------------------------
  const handleSearch = (e) => {
    setSearch(e.target.value);
    setPage(0);
  };

  // -------------------------------------------------------
  // Delete flow — confirm first, then delete
  // -------------------------------------------------------
  const handleDeleteConfirm = async () => {
    if (!deleteId) return;
    setDeleting(true);
    try {
      await customerApi.delete(deleteId);
      setSuccessMsg('Customer deleted successfully.');
      setDeleteId(null);
      loadCustomers();
      setTimeout(() => setSuccessMsg(''), 3000);
    } catch (err) {
      setError('Failed to delete customer.');
      setDeleteId(null);
    } finally {
      setDeleting(false);
    }
  };

  // -------------------------------------------------------
  // Format date of birth for display
  // -------------------------------------------------------
  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  };

  return (
    <div style={styles.page}>

      {/* ── Header ──────────────────────────────────────── */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Customers</h1>
          <p style={styles.subtitle}>
            {totalItems > 0 ? `${totalItems} total records` : 'No records found'}
          </p>
        </div>
        <div style={styles.headerActions}>
          <button
            style={styles.btnSecondary}
            onClick={() => onNavigate('bulk')}
          >
            ↑ Bulk Upload
          </button>
          <button
            style={styles.btnPrimary}
            onClick={() => onNavigate('create')}
          >
            + New Customer
          </button>
        </div>
      </div>

      {/* ── Success Message ──────────────────────────────── */}
      {successMsg && (
        <div style={styles.successBanner}>
          ✓ {successMsg}
        </div>
      )}

      {/* ── Error Message ────────────────────────────────── */}
      {error && (
        <div style={styles.errorBanner}>
          ✕ {error}
        </div>
      )}

      {/* ── Search Bar ───────────────────────────────────── */}
      <div style={styles.searchRow}>
        <div style={styles.searchWrapper}>
          <span style={styles.searchIcon}>⌕</span>
          <input
            style={styles.searchInput}
            type="text"
            placeholder="Search by name or NIC number..."
            value={search}
            onChange={handleSearch}
          />
          {search && (
            <button
              style={styles.clearBtn}
              onClick={() => { setSearch(''); setPage(0); }}
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* ── Table ────────────────────────────────────────── */}
      <div style={styles.tableWrapper}>
        {loading ? (
          <div style={styles.loadingState}>
            <div style={styles.spinner} />
            <p style={styles.loadingText}>Loading customers...</p>
          </div>
        ) : customers.length === 0 ? (
          <div style={styles.emptyState}>
            <div style={styles.emptyIcon}>👤</div>
            <p style={styles.emptyTitle}>No customers found</p>
            <p style={styles.emptySubtitle}>
              {search ? `No results for "${search}"` : 'Add your first customer to get started'}
            </p>
            {!search && (
              <button
                style={styles.btnPrimary}
                onClick={() => onNavigate('create')}
              >
                + Add Customer
              </button>
            )}
          </div>
        ) : (
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Name</th>
                <th style={styles.th}>NIC Number</th>
                <th style={styles.th}>Date of Birth</th>
                <th style={styles.th}>Mobile Numbers</th>
                <th style={styles.th}>Family Members</th>
                <th style={{ ...styles.th, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {customers.map((customer, index) => (
                <tr
                  key={customer.id}
                  style={{
                    ...styles.tr,
                    backgroundColor: index % 2 === 0 ? '#fff' : '#f9fafb',
                  }}
                  onMouseEnter={e => e.currentTarget.style.backgroundColor = '#f0f7ff'}
                  onMouseLeave={e => e.currentTarget.style.backgroundColor = index % 2 === 0 ? '#fff' : '#f9fafb'}
                >
                  {/* Name */}
                  <td style={styles.td}>
                    <div style={styles.nameCell}>
                      <div style={styles.avatar}>
                        {customer.name.charAt(0).toUpperCase()}
                      </div>
                      <span style={styles.nameTxt}>{customer.name}</span>
                    </div>
                  </td>

                  {/* NIC */}
                  <td style={styles.td}>
                    <span style={styles.nicBadge}>{customer.nicNumber}</span>
                  </td>

                  {/* DOB */}
                  <td style={styles.td}>
                    <span style={styles.dateTxt}>{formatDate(customer.dateOfBirth)}</span>
                  </td>

                  {/* Mobile Numbers */}
                  <td style={styles.td}>
                    {customer.mobileNumbers && customer.mobileNumbers.length > 0 ? (
                      <div style={styles.tagList}>
                        {customer.mobileNumbers.slice(0, 2).map((num, i) => (
                          <span key={i} style={styles.tag}>{num}</span>
                        ))}
                        {customer.mobileNumbers.length > 2 && (
                          <span style={styles.moreTag}>+{customer.mobileNumbers.length - 2}</span>
                        )}
                      </div>
                    ) : (
                      <span style={styles.emptyTxt}>—</span>
                    )}
                  </td>

                  {/* Family Members */}
                  <td style={styles.td}>
                    {customer.familyMembers && customer.familyMembers.length > 0 ? (
                      <span style={styles.familyCount}>
                        {customer.familyMembers.length} member{customer.familyMembers.length > 1 ? 's' : ''}
                      </span>
                    ) : (
                      <span style={styles.emptyTxt}>—</span>
                    )}
                  </td>

                  {/* Actions */}
                  <td style={{ ...styles.td, textAlign: 'right' }}>
                    <div style={styles.actionBtns}>
                      <button
                        style={styles.btnView}
                        onClick={() => onNavigate('view', customer.id)}
                        title="View details"
                      >
                        View
                      </button>
                      <button
                        style={styles.btnEdit}
                        onClick={() => onNavigate('edit', customer.id)}
                        title="Edit customer"
                      >
                        Edit
                      </button>
                      <button
                        style={styles.btnDelete}
                        onClick={() => setDeleteId(customer.id)}
                        title="Delete customer"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* ── Pagination ───────────────────────────────────── */}
      {totalPages > 1 && (
        <div style={styles.pagination}>
          <span style={styles.pageInfo}>
            Page {page + 1} of {totalPages}
          </span>
          <div style={styles.pageButtons}>
            <button
              style={styles.pageBtn}
              onClick={() => setPage(0)}
              disabled={page === 0}
            >
              «
            </button>
            <button
              style={styles.pageBtn}
              onClick={() => setPage(p => p - 1)}
              disabled={page === 0}
            >
              ‹ Prev
            </button>

            {/* Page number buttons — show 5 pages around current */}
            {Array.from({ length: totalPages }, (_, i) => i)
              .filter(i => Math.abs(i - page) <= 2)
              .map(i => (
                <button
                  key={i}
                  style={{
                    ...styles.pageBtn,
                    ...(i === page ? styles.pageBtnActive : {}),
                  }}
                  onClick={() => setPage(i)}
                >
                  {i + 1}
                </button>
              ))
            }

            <button
              style={styles.pageBtn}
              onClick={() => setPage(p => p + 1)}
              disabled={page >= totalPages - 1}
            >
              Next ›
            </button>
            <button
              style={styles.pageBtn}
              onClick={() => setPage(totalPages - 1)}
              disabled={page >= totalPages - 1}
            >
              »
            </button>
          </div>
        </div>
      )}

      {/* ── Delete Confirmation Modal ─────────────────────── */}
      {deleteId && (
        <div style={styles.modalOverlay}>
          <div style={styles.modal}>
            <div style={styles.modalIcon}>⚠</div>
            <h2 style={styles.modalTitle}>Delete Customer?</h2>
            <p style={styles.modalText}>
              This will permanently delete the customer and all their
              addresses, mobile numbers, and family member links.
              This action cannot be undone.
            </p>
            <div style={styles.modalActions}>
              <button
                style={styles.btnSecondary}
                onClick={() => setDeleteId(null)}
                disabled={deleting}
              >
                Cancel
              </button>
              <button
                style={styles.btnDanger}
                onClick={handleDeleteConfirm}
                disabled={deleting}
              >
                {deleting ? 'Deleting...' : 'Yes, Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ============================================================
//  STYLES
// ============================================================
const styles = {
  page: {
    padding: '32px',
    maxWidth: '1200px',
    margin: '0 auto',
    fontFamily: "'DM Sans', sans-serif",
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '24px',
  },
  title: {
    fontSize: '28px',
    fontWeight: '700',
    color: '#0f172a',
    margin: '0 0 4px 0',
  },
  subtitle: {
    fontSize: '14px',
    color: '#64748b',
    margin: 0,
  },
  headerActions: {
    display: 'flex',
    gap: '12px',
  },
  successBanner: {
    background: '#f0fdf4',
    border: '1px solid #86efac',
    color: '#166534',
    padding: '12px 16px',
    borderRadius: '8px',
    marginBottom: '16px',
    fontSize: '14px',
  },
  errorBanner: {
    background: '#fef2f2',
    border: '1px solid #fca5a5',
    color: '#991b1b',
    padding: '12px 16px',
    borderRadius: '8px',
    marginBottom: '16px',
    fontSize: '14px',
  },
  searchRow: {
    marginBottom: '20px',
  },
  searchWrapper: {
    position: 'relative',
    maxWidth: '480px',
  },
  searchIcon: {
    position: 'absolute',
    left: '14px',
    top: '50%',
    transform: 'translateY(-50%)',
    color: '#94a3b8',
    fontSize: '20px',
    pointerEvents: 'none',
  },
  searchInput: {
    width: '100%',
    padding: '10px 40px 10px 42px',
    border: '1px solid #e2e8f0',
    borderRadius: '10px',
    fontSize: '14px',
    color: '#0f172a',
    outline: 'none',
    boxSizing: 'border-box',
    transition: 'border-color 0.2s',
    backgroundColor: '#fff',
  },
  clearBtn: {
    position: 'absolute',
    right: '12px',
    top: '50%',
    transform: 'translateY(-50%)',
    background: 'none',
    border: 'none',
    color: '#94a3b8',
    cursor: 'pointer',
    fontSize: '14px',
    padding: '2px 4px',
  },
  tableWrapper: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    borderRadius: '12px',
    overflow: 'hidden',
    minHeight: '200px',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
  },
  th: {
    padding: '12px 16px',
    textAlign: 'left',
    fontSize: '12px',
    fontWeight: '600',
    color: '#475569',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    background: '#f8fafc',
    borderBottom: '1px solid #e2e8f0',
  },
  tr: {
    transition: 'background-color 0.15s',
  },
  td: {
    padding: '14px 16px',
    fontSize: '14px',
    color: '#1e293b',
    borderBottom: '1px solid #f1f5f9',
    verticalAlign: 'middle',
  },
  nameCell: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  avatar: {
    width: '36px',
    height: '36px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #3b82f6, #6366f1)',
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '14px',
    fontWeight: '600',
    flexShrink: 0,
  },
  nameTxt: {
    fontWeight: '500',
    color: '#0f172a',
  },
  nicBadge: {
    display: 'inline-block',
    background: '#f1f5f9',
    border: '1px solid #e2e8f0',
    color: '#475569',
    padding: '2px 10px',
    borderRadius: '6px',
    fontSize: '13px',
    fontFamily: 'monospace',
    letterSpacing: '0.03em',
  },
  dateTxt: {
    color: '#475569',
  },
  tagList: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '4px',
  },
  tag: {
    background: '#eff6ff',
    color: '#3b82f6',
    border: '1px solid #bfdbfe',
    padding: '2px 8px',
    borderRadius: '4px',
    fontSize: '12px',
  },
  moreTag: {
    background: '#f1f5f9',
    color: '#64748b',
    border: '1px solid #e2e8f0',
    padding: '2px 8px',
    borderRadius: '4px',
    fontSize: '12px',
  },
  familyCount: {
    background: '#f0fdf4',
    color: '#16a34a',
    border: '1px solid #bbf7d0',
    padding: '2px 10px',
    borderRadius: '4px',
    fontSize: '12px',
  },
  emptyTxt: {
    color: '#cbd5e1',
  },
  actionBtns: {
    display: 'flex',
    gap: '6px',
    justifyContent: 'flex-end',
  },
  btnPrimary: {
    background: '#3b82f6',
    color: '#fff',
    border: 'none',
    padding: '10px 20px',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '500',
    cursor: 'pointer',
  },
  btnSecondary: {
    background: '#fff',
    color: '#374151',
    border: '1px solid #d1d5db',
    padding: '10px 20px',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '500',
    cursor: 'pointer',
  },
  btnView: {
    background: '#f8fafc',
    color: '#475569',
    border: '1px solid #e2e8f0',
    padding: '6px 12px',
    borderRadius: '6px',
    fontSize: '13px',
    cursor: 'pointer',
  },
  btnEdit: {
    background: '#eff6ff',
    color: '#3b82f6',
    border: '1px solid #bfdbfe',
    padding: '6px 12px',
    borderRadius: '6px',
    fontSize: '13px',
    cursor: 'pointer',
  },
  btnDelete: {
    background: '#fef2f2',
    color: '#ef4444',
    border: '1px solid #fecaca',
    padding: '6px 12px',
    borderRadius: '6px',
    fontSize: '13px',
    cursor: 'pointer',
  },
  btnDanger: {
    background: '#ef4444',
    color: '#fff',
    border: 'none',
    padding: '10px 20px',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '500',
    cursor: 'pointer',
  },
  loadingState: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '60px 20px',
    gap: '16px',
  },
  spinner: {
    width: '36px',
    height: '36px',
    border: '3px solid #e2e8f0',
    borderTop: '3px solid #3b82f6',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
  loadingText: {
    color: '#64748b',
    fontSize: '14px',
    margin: 0,
  },
  emptyState: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '60px 20px',
    gap: '8px',
  },
  emptyIcon: {
    fontSize: '48px',
    marginBottom: '8px',
  },
  emptyTitle: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#0f172a',
    margin: '0 0 4px 0',
  },
  emptySubtitle: {
    fontSize: '14px',
    color: '#64748b',
    margin: '0 0 16px 0',
  },
  pagination: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: '20px',
    padding: '0 4px',
  },
  pageInfo: {
    fontSize: '14px',
    color: '#64748b',
  },
  pageButtons: {
    display: 'flex',
    gap: '4px',
  },
  pageBtn: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    color: '#374151',
    padding: '6px 12px',
    borderRadius: '6px',
    fontSize: '13px',
    cursor: 'pointer',
    transition: 'all 0.15s',
  },
  pageBtnActive: {
    background: '#3b82f6',
    color: '#fff',
    border: '1px solid #3b82f6',
  },
  modalOverlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  modal: {
    background: '#fff',
    borderRadius: '16px',
    padding: '32px',
    maxWidth: '420px',
    width: '90%',
    textAlign: 'center',
    boxShadow: '0 20px 60px rgba(0,0,0,0.15)',
  },
  modalIcon: {
    fontSize: '40px',
    marginBottom: '12px',
    color: '#f59e0b',
  },
  modalTitle: {
    fontSize: '20px',
    fontWeight: '700',
    color: '#0f172a',
    margin: '0 0 12px 0',
  },
  modalText: {
    fontSize: '14px',
    color: '#475569',
    lineHeight: '1.6',
    margin: '0 0 24px 0',
  },
  modalActions: {
    display: 'flex',
    gap: '12px',
    justifyContent: 'center',
  },
};

// Inject spinner animation into the page
const styleTag = document.createElement('style');
styleTag.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
document.head.appendChild(styleTag);