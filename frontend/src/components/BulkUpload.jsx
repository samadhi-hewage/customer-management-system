import { useState, useRef, useEffect, useCallback } from 'react';
import { bulkApi } from '../api/axios';

// ============================================================
//  BulkUpload.jsx
//  Location: src/components/BulkUpload.jsx
//
//  Features:
//    - Drag and drop OR click to select .xlsx / .xls file
//    - File size and type validation before upload
//    - Upload progress bar (tracks file transfer to server)
//    - Processing progress bar (tracks backend SAX parsing)
//    - Polls /api/bulk/status/{jobId} every 2 seconds
//    - Shows success / failed row counts when done
//    - Keeps a history of past jobs in this session
//    - Download template button (shows required columns)
// ============================================================

const STATUS_COLORS = {
  PENDING:    { bg: '#fef9c3', text: '#854d0e', border: '#fde047' },
  PROCESSING: { bg: '#eff6ff', text: '#1d4ed8', border: '#93c5fd' },
  DONE:       { bg: '#f0fdf4', text: '#166534', border: '#86efac' },
  FAILED:     { bg: '#fef2f2', text: '#991b1b', border: '#fca5a5' },
};

const STATUS_LABELS = {
  PENDING:    '⏳ Pending',
  PROCESSING: '⚙️ Processing',
  DONE:       '✅ Complete',
  FAILED:     '❌ Failed',
};

export default function BulkUpload({ onNavigate }) {
  // ── File selection ────────────────────────────────────────
  const [selectedFile, setSelectedFile]   = useState(null);
  const [dragOver, setDragOver]           = useState(false);
  const fileInputRef                      = useRef(null);

  // ── Upload phase (file → server) ─────────────────────────
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploading, setUploading]           = useState(false);

  // ── Processing phase (server parsing Excel) ───────────────
  const [currentJob, setCurrentJob]   = useState(null);   // current job status object
  const [jobId, setJobId]             = useState(null);
  const pollRef                       = useRef(null);

  // ── Job history (this session only) ──────────────────────
  const [jobHistory, setJobHistory]   = useState([]);

  // ── Error / general messages ─────────────────────────────
  const [error, setError]             = useState('');

  // ─────────────────────────────────────────────────────────
  // Cleanup polling on unmount
  // ─────────────────────────────────────────────────────────
  useEffect(() => {
    return () => { if (pollRef.current) clearInterval(pollRef.current); };
  }, []);

  // ─────────────────────────────────────────────────────────
  // Start polling job status every 2 seconds
  // ─────────────────────────────────────────────────────────
  const startPolling = useCallback((id) => {
    if (pollRef.current) clearInterval(pollRef.current);

    pollRef.current = setInterval(async () => {
      try {
        const res  = await bulkApi.getStatus(id);
        const job  = res.data;
        setCurrentJob(job);

        // Update in history too
        setJobHistory(prev =>
          prev.map(h => h.jobId === id ? { ...h, ...job } : h)
        );

        // Stop polling when terminal state reached
        if (job.status === 'DONE' || job.status === 'FAILED') {
          clearInterval(pollRef.current);
          pollRef.current = null;
        }
      } catch {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
    }, 2000);
  }, []);

  // ─────────────────────────────────────────────────────────
  // File validation
  // ─────────────────────────────────────────────────────────
  const validateFile = (file) => {
    if (!file) return 'No file selected.';
    const name = file.name.toLowerCase();
    if (!name.endsWith('.xlsx') && !name.endsWith('.xls'))
      return 'Only Excel files (.xlsx or .xls) are accepted.';
    if (file.size > 200 * 1024 * 1024)
      return 'File is too large. Maximum size is 200MB.';
    return null;
  };

  // ─────────────────────────────────────────────────────────
  // Handle file selection (from input or drag-drop)
  // ─────────────────────────────────────────────────────────
  const handleFileSelect = (file) => {
    setError('');
    const err = validateFile(file);
    if (err) { setError(err); return; }
    setSelectedFile(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    handleFileSelect(file);
  };

  const handleInputChange = (e) => {
    handleFileSelect(e.target.files[0]);
    e.target.value = ''; // reset so same file can be re-selected
  };

  // ─────────────────────────────────────────────────────────
  // Upload the file
  // ─────────────────────────────────────────────────────────
  const handleUpload = async () => {
    if (!selectedFile) { setError('Please select a file first.'); return; }
    setError('');
    setUploading(true);
    setUploadProgress(0);
    setCurrentJob(null);

    try {
      // Phase 1: upload file to server (tracked via onUploadProgress)
      const res = await bulkApi.upload(selectedFile, (evt) => {
        if (evt.total) {
          setUploadProgress(Math.round((evt.loaded * 100) / evt.total));
        }
      });

      const newJobId = res.data.jobId;
      setJobId(newJobId);
      setSelectedFile(null);

      // Add to history immediately
      const historyEntry = {
        jobId:     newJobId,
        fileName:  selectedFile.name,
        status:    'PENDING',
        processed: 0,
        failed:    0,
        totalRows: 0,
        percent:   0,
        startedAt: new Date().toLocaleTimeString(),
      };
      setJobHistory(prev => [historyEntry, ...prev]);

      // Phase 2: start polling for backend processing progress
      startPolling(newJobId);

    } catch (err) {
      setError(err.response?.data?.error || 'Upload failed. Is the backend running?');
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  // ─────────────────────────────────────────────────────────
  // Reset to upload another file
  // ─────────────────────────────────────────────────────────
  const handleReset = () => {
    if (pollRef.current) clearInterval(pollRef.current);
    setSelectedFile(null);
    setCurrentJob(null);
    setJobId(null);
    setError('');
    setUploadProgress(0);
  };

  // ─────────────────────────────────────────────────────────
  // Format file size
  // ─────────────────────────────────────────────────────────
  const formatSize = (bytes) => {
    if (bytes < 1024)        return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const isProcessing = currentJob &&
    (currentJob.status === 'PENDING' || currentJob.status === 'PROCESSING');

  // ─────────────────────────────────────────────────────────
  // Render
  // ─────────────────────────────────────────────────────────
  return (
    <div style={S.page}>

      {/* ── Page header ──────────────────────────────────── */}
      <div style={S.header}>
        <div>
          <h1 style={S.title}>Bulk Upload</h1>
          <p style={S.subtitle}>
            Upload an Excel file to create or update thousands of customers at once
          </p>
        </div>
        <button style={S.backBtn} onClick={() => onNavigate('list')}>
          ← Back to list
        </button>
      </div>

      <div style={S.layout}>
        <div style={S.mainCol}>

          {/* ── Template instructions card ──────────────── */}
          <div style={S.infoCard}>
            <div style={S.infoCardHeader}>
              <span style={S.infoIcon}>📋</span>
              <div>
                <h2 style={S.infoTitle}>Required Excel Format</h2>
                <p style={S.infoSubtitle}>Your file must have these exact columns in order</p>
              </div>
            </div>
            <table style={S.colTable}>
              <thead>
                <tr>
                  <th style={S.colTh}>Column</th>
                  <th style={S.colTh}>Header name</th>
                  <th style={S.colTh}>Format</th>
                  <th style={S.colTh}>Required</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style={S.colTd}><span style={S.colBadge}>A</span></td>
                  <td style={S.colTd}><code style={S.code}>Name</code></td>
                  <td style={S.colTd}>Any text</td>
                  <td style={S.colTd}><span style={S.reqBadge}>Required</span></td>
                </tr>
                <tr style={{ background: '#f8fafc' }}>
                  <td style={S.colTd}><span style={S.colBadge}>B</span></td>
                  <td style={S.colTd}><code style={S.code}>Date of Birth</code></td>
                  <td style={S.colTd}>yyyy-MM-dd (e.g. 1990-03-15)</td>
                  <td style={S.colTd}><span style={S.reqBadge}>Required</span></td>
                </tr>
                <tr>
                  <td style={S.colTd}><span style={S.colBadge}>C</span></td>
                  <td style={S.colTd}><code style={S.code}>NIC Number</code></td>
                  <td style={S.colTd}>Unique (e.g. 900751234V)</td>
                  <td style={S.colTd}><span style={S.reqBadge}>Required</span></td>
                </tr>
              </tbody>
            </table>
            <p style={S.infoNote}>
              💡 If a NIC already exists in the database, that row will be <strong>updated</strong> instead of skipped.
              Rows with missing mandatory fields are skipped and counted as failed.
            </p>
          </div>

          {/* ── Drop zone ────────────────────────────────── */}
          {!currentJob && (
            <div
              style={{
                ...S.dropZone,
                ...(dragOver ? S.dropZoneActive : {}),
                ...(selectedFile ? S.dropZoneSelected : {}),
              }}
              onDragOver={e => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={handleDrop}
              onClick={() => !selectedFile && fileInputRef.current?.click()}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx,.xls"
                style={{ display: 'none' }}
                onChange={handleInputChange}
              />

              {!selectedFile ? (
                <>
                  <div style={S.dropIcon}>📂</div>
                  <p style={S.dropTitle}>
                    {dragOver ? 'Drop it here!' : 'Drag & drop your Excel file'}
                  </p>
                  <p style={S.dropSubtitle}>or click to browse</p>
                  <span style={S.dropHint}>.xlsx or .xls · max 200 MB</span>
                </>
              ) : (
                <>
                  <div style={S.filePreview}>
                    <span style={S.fileIcon}>📊</span>
                    <div style={S.fileInfo}>
                      <p style={S.fileName}>{selectedFile.name}</p>
                      <p style={S.fileSize}>{formatSize(selectedFile.size)}</p>
                    </div>
                    <button
                      style={S.removeFileBtn}
                      onClick={e => { e.stopPropagation(); setSelectedFile(null); setError(''); }}
                    >
                      ✕
                    </button>
                  </div>
                </>
              )}
            </div>
          )}

          {/* ── Error message ────────────────────────────── */}
          {error && (
            <div style={S.errorBanner}>✕ {error}</div>
          )}

          {/* ── Upload progress (file transfer) ──────────── */}
          {uploading && (
            <div style={S.progressCard}>
              <div style={S.progressHeader}>
                <span style={S.progressLabel}>Uploading file to server...</span>
                <span style={S.progressPct}>{uploadProgress}%</span>
              </div>
              <div style={S.progressTrack}>
                <div style={{ ...S.progressFill, width: `${uploadProgress}%`, background: '#3b82f6' }} />
              </div>
            </div>
          )}

          {/* ── Processing progress (backend parsing) ────── */}
          {currentJob && (
            <div style={S.jobCard}>
              <div style={S.jobCardHeader}>
                <div>
                  <p style={S.jobFileName}>{currentJob.fileName}</p>
                  <span style={{
                    ...S.statusBadge,
                    background: STATUS_COLORS[currentJob.status]?.bg,
                    color:      STATUS_COLORS[currentJob.status]?.text,
                    border:     `1px solid ${STATUS_COLORS[currentJob.status]?.border}`,
                  }}>
                    {STATUS_LABELS[currentJob.status]}
                  </span>
                </div>
                {(currentJob.status === 'DONE' || currentJob.status === 'FAILED') && (
                  <button style={S.uploadAnotherBtn} onClick={handleReset}>
                    Upload another file
                  </button>
                )}
              </div>

              {/* Processing progress bar */}
              <div style={S.progressCard}>
                <div style={S.progressHeader}>
                  <span style={S.progressLabel}>
                    {isProcessing ? 'Processing rows...' : 'Processing complete'}
                  </span>
                  <span style={S.progressPct}>{currentJob.percent ?? 0}%</span>
                </div>
                <div style={S.progressTrack}>
                  <div style={{
                    ...S.progressFill,
                    width: `${currentJob.percent ?? 0}%`,
                    background: currentJob.status === 'FAILED' ? '#ef4444'
                              : currentJob.status === 'DONE'   ? '#22c55e'
                              : '#3b82f6',
                    transition: 'width 0.5s ease',
                  }} />
                </div>
              </div>

              {/* Stats row */}
              <div style={S.statsRow}>
                <div style={S.statBox}>
                  <span style={S.statNum}>{currentJob.totalRows?.toLocaleString() || 0}</span>
                  <span style={S.statLabel}>Total rows</span>
                </div>
                <div style={{ ...S.statBox, borderColor: '#86efac' }}>
                  <span style={{ ...S.statNum, color: '#16a34a' }}>
                    {currentJob.processed?.toLocaleString() || 0}
                  </span>
                  <span style={S.statLabel}>Processed</span>
                </div>
                <div style={{ ...S.statBox, borderColor: '#fca5a5' }}>
                  <span style={{ ...S.statNum, color: '#dc2626' }}>
                    {currentJob.failed?.toLocaleString() || 0}
                  </span>
                  <span style={S.statLabel}>Failed / skipped</span>
                </div>
                <div style={S.statBox}>
                  <span style={{ ...S.statNum, color: '#7c3aed' }}>
                    {currentJob.totalRows > 0
                      ? Math.round((currentJob.processed / currentJob.totalRows) * 100) + '%'
                      : '—'}
                  </span>
                  <span style={S.statLabel}>Success rate</span>
                </div>
              </div>

              {/* Error message if failed */}
              {currentJob.status === 'FAILED' && currentJob.errorMsg && (
                <div style={S.jobError}>
                  <strong>Error:</strong> {currentJob.errorMsg}
                </div>
              )}

              {/* Done — link to view customers */}
              {currentJob.status === 'DONE' && (
                <div style={S.successNote}>
                  🎉 Upload complete! {currentJob.processed?.toLocaleString()} customers have been created or updated.
                  <button style={S.viewListBtn} onClick={() => onNavigate('list')}>
                    View customers →
                  </button>
                </div>
              )}

              {/* Polling indicator */}
              {isProcessing && (
                <div style={S.pollingNote}>
                  <span style={S.dot} /> Checking progress every 2 seconds...
                </div>
              )}
            </div>
          )}

          {/* ── Upload button ────────────────────────────── */}
          {!currentJob && (
            <button
              style={{
                ...S.uploadBtn,
                opacity: (!selectedFile || uploading) ? 0.5 : 1,
                cursor:  (!selectedFile || uploading) ? 'not-allowed' : 'pointer',
              }}
              onClick={handleUpload}
              disabled={!selectedFile || uploading}
            >
              {uploading ? 'Uploading...' : '↑ Upload and Process'}
            </button>
          )}

        </div>

        {/* ── Job history sidebar ───────────────────────── */}
        <div style={S.sideCol}>
          <div style={S.historyCard}>
            <h3 style={S.historyTitle}>Session history</h3>
            {jobHistory.length === 0 ? (
              <p style={S.historyEmpty}>No uploads yet this session.</p>
            ) : (
              <div style={S.historyList}>
                {jobHistory.map((job) => (
                  <div key={job.jobId} style={S.historyItem}>
                    <div style={S.historyItemTop}>
                      <span style={S.historyFileName}>{job.fileName}</span>
                      <span style={{
                        ...S.historyStatus,
                        color: STATUS_COLORS[job.status]?.text,
                      }}>
                        {STATUS_LABELS[job.status]}
                      </span>
                    </div>
                    <div style={S.historyStats}>
                      <span>✓ {job.processed?.toLocaleString() || 0}</span>
                      <span style={{ color: '#dc2626' }}>
                        ✕ {job.failed?.toLocaleString() || 0}
                      </span>
                      <span style={{ color: '#94a3b8' }}>{job.startedAt}</span>
                    </div>
                    {/* Mini progress bar */}
                    <div style={S.miniTrack}>
                      <div style={{
                        ...S.miniFill,
                        width: `${job.percent ?? 0}%`,
                        background: job.status === 'FAILED' ? '#ef4444'
                                  : job.status === 'DONE'   ? '#22c55e'
                                  : '#3b82f6',
                      }} />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ============================================================
//  STYLES
// ============================================================
const S = {
  page: {
    padding: '32px',
    maxWidth: '1100px',
    margin: '0 auto',
    fontFamily: "'DM Sans', sans-serif",
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '28px',
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
  backBtn: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    color: '#475569',
    padding: '8px 16px',
    borderRadius: '8px',
    cursor: 'pointer',
    fontSize: '14px',
  },
  layout: {
    display: 'grid',
    gridTemplateColumns: '1fr 300px',
    gap: '24px',
    alignItems: 'start',
  },
  mainCol: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
  },
  sideCol: {
    position: 'sticky',
    top: '80px',
  },

  // Info card
  infoCard: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    borderRadius: '14px',
    padding: '24px',
  },
  infoCardHeader: {
    display: 'flex',
    gap: '14px',
    alignItems: 'center',
    marginBottom: '20px',
  },
  infoIcon: { fontSize: '28px' },
  infoTitle: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#0f172a',
    margin: '0 0 2px 0',
  },
  infoSubtitle: { fontSize: '13px', color: '#94a3b8', margin: 0 },
  colTable: {
    width: '100%',
    borderCollapse: 'collapse',
    marginBottom: '16px',
  },
  colTh: {
    padding: '8px 12px',
    textAlign: 'left',
    fontSize: '11px',
    fontWeight: '600',
    color: '#64748b',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    background: '#f8fafc',
    borderBottom: '1px solid #e2e8f0',
  },
  colTd: {
    padding: '10px 12px',
    fontSize: '13px',
    color: '#1e293b',
    borderBottom: '1px solid #f1f5f9',
  },
  colBadge: {
    display: 'inline-block',
    background: '#3b82f6',
    color: '#fff',
    width: '22px',
    height: '22px',
    borderRadius: '4px',
    textAlign: 'center',
    lineHeight: '22px',
    fontSize: '12px',
    fontWeight: '700',
  },
  reqBadge: {
    background: '#fef2f2',
    color: '#dc2626',
    border: '1px solid #fecaca',
    padding: '2px 8px',
    borderRadius: '4px',
    fontSize: '12px',
  },
  code: {
    background: '#f1f5f9',
    padding: '2px 6px',
    borderRadius: '4px',
    fontFamily: 'monospace',
    fontSize: '13px',
  },
  infoNote: {
    fontSize: '13px',
    color: '#475569',
    background: '#f8fafc',
    border: '1px solid #e2e8f0',
    borderRadius: '8px',
    padding: '12px',
    margin: 0,
    lineHeight: 1.6,
  },

  // Drop zone
  dropZone: {
    border: '2px dashed #cbd5e1',
    borderRadius: '14px',
    padding: '48px 24px',
    textAlign: 'center',
    cursor: 'pointer',
    background: '#f8fafc',
    transition: 'all 0.2s',
  },
  dropZoneActive: {
    border: '2px dashed #3b82f6',
    background: '#eff6ff',
    transform: 'scale(1.01)',
  },
  dropZoneSelected: {
    border: '2px solid #22c55e',
    background: '#f0fdf4',
    cursor: 'default',
    padding: '24px',
  },
  dropIcon: { fontSize: '48px', marginBottom: '12px' },
  dropTitle: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#0f172a',
    margin: '0 0 4px 0',
  },
  dropSubtitle: { fontSize: '14px', color: '#64748b', margin: '0 0 12px 0' },
  dropHint: {
    fontSize: '12px',
    color: '#94a3b8',
    background: '#f1f5f9',
    padding: '4px 12px',
    borderRadius: '20px',
    display: 'inline-block',
  },
  filePreview: {
    display: 'flex',
    alignItems: 'center',
    gap: '14px',
  },
  fileIcon: { fontSize: '36px' },
  fileInfo: { flex: 1, textAlign: 'left' },
  fileName: {
    fontSize: '15px',
    fontWeight: '600',
    color: '#0f172a',
    margin: '0 0 2px 0',
    wordBreak: 'break-all',
  },
  fileSize: { fontSize: '13px', color: '#64748b', margin: 0 },
  removeFileBtn: {
    background: '#fef2f2',
    border: '1px solid #fecaca',
    color: '#ef4444',
    width: '30px',
    height: '30px',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '12px',
    flexShrink: 0,
  },

  // Error
  errorBanner: {
    background: '#fef2f2',
    border: '1px solid #fca5a5',
    color: '#991b1b',
    padding: '12px 16px',
    borderRadius: '8px',
    fontSize: '14px',
  },

  // Progress bars
  progressCard: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    borderRadius: '10px',
    padding: '16px',
  },
  progressHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    marginBottom: '10px',
  },
  progressLabel: { fontSize: '13px', color: '#475569' },
  progressPct: { fontSize: '13px', fontWeight: '600', color: '#0f172a' },
  progressTrack: {
    height: '8px',
    background: '#f1f5f9',
    borderRadius: '4px',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    borderRadius: '4px',
    transition: 'width 0.3s ease',
  },

  // Job card
  jobCard: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    borderRadius: '14px',
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  jobCardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  jobFileName: {
    fontSize: '15px',
    fontWeight: '600',
    color: '#0f172a',
    margin: '0 0 6px 0',
    wordBreak: 'break-all',
  },
  statusBadge: {
    display: 'inline-block',
    padding: '3px 10px',
    borderRadius: '20px',
    fontSize: '12px',
    fontWeight: '500',
  },
  statsRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap: '12px',
  },
  statBox: {
    border: '1px solid #e2e8f0',
    borderRadius: '10px',
    padding: '14px',
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
  },
  statNum: {
    fontSize: '22px',
    fontWeight: '700',
    color: '#0f172a',
  },
  statLabel: {
    fontSize: '11px',
    color: '#94a3b8',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
  },
  jobError: {
    background: '#fef2f2',
    border: '1px solid #fca5a5',
    color: '#991b1b',
    padding: '12px',
    borderRadius: '8px',
    fontSize: '13px',
  },
  successNote: {
    background: '#f0fdf4',
    border: '1px solid #86efac',
    color: '#166534',
    padding: '14px 16px',
    borderRadius: '8px',
    fontSize: '14px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: '10px',
  },
  viewListBtn: {
    background: '#16a34a',
    color: '#fff',
    border: 'none',
    padding: '8px 16px',
    borderRadius: '6px',
    fontSize: '13px',
    cursor: 'pointer',
    fontWeight: '500',
  },
  pollingNote: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '12px',
    color: '#94a3b8',
  },
  dot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    background: '#3b82f6',
    display: 'inline-block',
    animation: 'pulse 1.5s ease-in-out infinite',
  },
  uploadAnotherBtn: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    color: '#374151',
    padding: '8px 16px',
    borderRadius: '8px',
    fontSize: '13px',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },

  // Upload button
  uploadBtn: {
    background: '#3b82f6',
    color: '#fff',
    border: 'none',
    padding: '14px',
    borderRadius: '10px',
    fontSize: '15px',
    fontWeight: '600',
    width: '100%',
    transition: 'opacity 0.2s',
  },

  // History sidebar
  historyCard: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    borderRadius: '14px',
    padding: '20px',
  },
  historyTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#0f172a',
    margin: '0 0 16px 0',
    paddingBottom: '12px',
    borderBottom: '1px solid #f1f5f9',
  },
  historyEmpty: {
    fontSize: '13px',
    color: '#94a3b8',
    textAlign: 'center',
    padding: '16px 0',
    margin: 0,
  },
  historyList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  historyItem: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  historyItemTop: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '8px',
  },
  historyFileName: {
    fontSize: '12px',
    fontWeight: '500',
    color: '#0f172a',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    flex: 1,
  },
  historyStatus: {
    fontSize: '11px',
    fontWeight: '500',
    whiteSpace: 'nowrap',
  },
  historyStats: {
    display: 'flex',
    gap: '10px',
    fontSize: '11px',
    color: '#64748b',
  },
  miniTrack: {
    height: '4px',
    background: '#f1f5f9',
    borderRadius: '2px',
    overflow: 'hidden',
  },
  miniFill: {
    height: '100%',
    borderRadius: '2px',
    transition: 'width 0.5s ease',
  },
};

// Inject animations
const styleTag = document.createElement('style');
styleTag.textContent = `
  @keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.5; transform: scale(0.8); }
  }
`;
document.head.appendChild(styleTag);