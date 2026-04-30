import { useState, useEffect } from 'react';
import { customerApi, masterApi } from '../api/axios';

// ============================================================
//  CustomerForm.jsx
//  Location: src/components/CustomerForm.jsx
//
//  Works in two modes:
//    CREATE: onNavigate('create')        → customerId is null
//    EDIT:   onNavigate('edit', id)      → customerId is the ID
//
//  Fields:
//    - Name (mandatory)
//    - Date of Birth (date picker, mandatory)
//    - NIC Number (mandatory, unique)
//    - Mobile Numbers (multiple, add/remove)
//    - Addresses (multiple, with city/country dropdowns)
//    - Family Members (search existing customers by NIC)
// ============================================================

export default function CustomerForm({ customerId, onNavigate }) {
  const isEdit = !!customerId;

  // ── Form state ────────────────────────────────────────────
  const [form, setForm] = useState({
    name:          '',
    dateOfBirth:   '',
    nicNumber:     '',
    mobileNumbers: [''],
    addresses:     [],
    familyMembers: [],
  });

  // ── UI state ──────────────────────────────────────────────
  const [errors, setErrors]           = useState({});
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting]   = useState(false);
  const [loading, setLoading]         = useState(isEdit);

  // ── Master data ───────────────────────────────────────────
  const [countries, setCountries]     = useState([]);

  // ── Family member search ──────────────────────────────────
  const [familySearch, setFamilySearch]   = useState('');
  const [familyResults, setFamilyResults] = useState([]);
  const [searchingFamily, setSearchingFamily] = useState(false);

  // ─────────────────────────────────────────────────────────
  // On mount: load countries and (if edit) load customer data
  // ─────────────────────────────────────────────────────────
  useEffect(() => {
    masterApi.getCountries()
      .then(res => setCountries(res.data))
      .catch(() => {});

    if (isEdit) {
      customerApi.getById(customerId)
        .then(res => {
          const c = res.data;
          setForm({
            name:          c.name || '',
            dateOfBirth:   c.dateOfBirth || '',
            nicNumber:     c.nicNumber || '',
            mobileNumbers: c.mobileNumbers?.length ? c.mobileNumbers : [''],
            addresses:     c.addresses?.map(a => ({
              id:           a.id,
              addressLine1: a.addressLine1 || '',
              addressLine2: a.addressLine2 || '',
              countryId:    a.countryId || '',
              cityId:       a.cityId || '',
              cityName:     a.cityName || '',
              countryName:  a.countryName || '',
              cities:       [],  // loaded when country selected
            })) || [],
            familyMembers: c.familyMembers || [],
          });
          // Load cities for each address that already has a country
          c.addresses?.forEach((addr, i) => {
            if (addr.countryId) loadCitiesForAddress(i, addr.countryId);
          });
        })
        .catch(() => setServerError('Failed to load customer data.'))
        .finally(() => setLoading(false));
    }
  }, [customerId]);

  // ─────────────────────────────────────────────────────────
  // Load cities when country changes on an address row
  // ─────────────────────────────────────────────────────────
  const loadCitiesForAddress = (index, countryId) => {
    masterApi.getCitiesByCountry(countryId).then(res => {
      setForm(prev => {
        const addrs = [...prev.addresses];
        addrs[index] = { ...addrs[index], cities: res.data };
        return { ...prev, addresses: addrs };
      });
    }).catch(() => {});
  };

  // ─────────────────────────────────────────────────────────
  // Basic field change
  // ─────────────────────────────────────────────────────────
  const handleField = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }));
    setErrors(prev => ({ ...prev, [field]: '' }));
  };

  // ─────────────────────────────────────────────────────────
  // Mobile numbers — add / change / remove
  // ─────────────────────────────────────────────────────────
  const addMobile = () =>
    setForm(prev => ({ ...prev, mobileNumbers: [...prev.mobileNumbers, ''] }));

  const updateMobile = (index, value) => {
    const updated = [...form.mobileNumbers];
    updated[index] = value;
    setForm(prev => ({ ...prev, mobileNumbers: updated }));
  };

  const removeMobile = (index) => {
    setForm(prev => ({
      ...prev,
      mobileNumbers: prev.mobileNumbers.filter((_, i) => i !== index),
    }));
  };

  // ─────────────────────────────────────────────────────────
  // Addresses — add / change / remove
  // ─────────────────────────────────────────────────────────
  const addAddress = () =>
    setForm(prev => ({
      ...prev,
      addresses: [...prev.addresses, {
        addressLine1: '', addressLine2: '',
        countryId: '', cityId: '', cities: [],
      }],
    }));

  const updateAddress = (index, field, value) => {
    const updated = [...form.addresses];
    updated[index] = { ...updated[index], [field]: value };
    // When country changes → reset city and reload city list
    if (field === 'countryId') {
      updated[index].cityId = '';
      updated[index].cities = [];
      if (value) loadCitiesForAddress(index, value);
    }
    setForm(prev => ({ ...prev, addresses: updated }));
  };

  const removeAddress = (index) =>
    setForm(prev => ({
      ...prev,
      addresses: prev.addresses.filter((_, i) => i !== index),
    }));

  // ─────────────────────────────────────────────────────────
  // Family members — search by name/NIC, add, remove
  // ─────────────────────────────────────────────────────────
  const searchFamilyMembers = async (term) => {
    setFamilySearch(term);
    if (!term.trim()) { setFamilyResults([]); return; }
    setSearchingFamily(true);
    try {
      const res = await customerApi.getAll(term, 0, 5);
      // Filter out self and already-added members
      const existing = new Set(form.familyMembers.map(f => f.id));
      setFamilyResults(
        res.data.content.filter(c =>
          c.id !== Number(customerId) && !existing.has(c.id)
        )
      );
    } catch {}
    finally { setSearchingFamily(false); }
  };

  const addFamilyMember = (customer) => {
    setForm(prev => ({
      ...prev,
      familyMembers: [...prev.familyMembers, {
        id: customer.id, name: customer.name, nicNumber: customer.nicNumber,
      }],
    }));
    setFamilySearch('');
    setFamilyResults([]);
  };

  const removeFamilyMember = (id) =>
    setForm(prev => ({
      ...prev,
      familyMembers: prev.familyMembers.filter(f => f.id !== id),
    }));

  // ─────────────────────────────────────────────────────────
  // Validation
  // ─────────────────────────────────────────────────────────
  const validate = () => {
    const errs = {};
    if (!form.name.trim())        errs.name = 'Name is required';
    if (!form.dateOfBirth)        errs.dateOfBirth = 'Date of birth is required';
    if (!form.nicNumber.trim())   errs.nicNumber = 'NIC number is required';
    form.addresses.forEach((addr, i) => {
      if (!addr.addressLine1.trim())
        errs[`addr_${i}_line1`] = 'Address line 1 is required';
    });
    return errs;
  };

  // ─────────────────────────────────────────────────────────
  // Submit
  // ─────────────────────────────────────────────────────────
  const handleSubmit = async () => {
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }

    setSubmitting(true);
    setServerError('');

    const payload = {
      name:          form.name.trim(),
      dateOfBirth:   form.dateOfBirth,
      nicNumber:     form.nicNumber.trim(),
      mobileNumbers: form.mobileNumbers.filter(m => m.trim()),
      addresses:     form.addresses.map(a => ({
        id:           a.id,
        addressLine1: a.addressLine1.trim(),
        addressLine2: a.addressLine2?.trim() || '',
        cityId:       a.cityId ? Number(a.cityId) : null,
        countryId:    a.countryId ? Number(a.countryId) : null,
      })),
      familyMembers: form.familyMembers.map(f => ({ id: f.id })),
    };

    try {
      if (isEdit) {
        await customerApi.update(customerId, payload);
      } else {
        await customerApi.create(payload);
      }
      onNavigate('list');
    } catch (err) {
      const data = err.response?.data;
      if (data?.fields) {
        // Map server validation errors to our error state
        const mapped = {};
        Object.entries(data.fields).forEach(([k, v]) => { mapped[k] = v; });
        setErrors(mapped);
      } else {
        setServerError(data?.message || 'Something went wrong. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  // ─────────────────────────────────────────────────────────
  // Loading state
  // ─────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div style={S.loadingWrap}>
        <div style={S.spinner} />
        <p style={{ color: '#64748b', marginTop: 16 }}>Loading customer...</p>
      </div>
    );
  }

  // ─────────────────────────────────────────────────────────
  // Render
  // ─────────────────────────────────────────────────────────
  return (
    <div style={S.page}>

      {/* ── Page header ──────────────────────────────────── */}
      <div style={S.pageHeader}>
        <button style={S.backBtn} onClick={() => onNavigate('list')}>
          ← Back
        </button>
        <div>
          <h1 style={S.pageTitle}>
            {isEdit ? 'Edit Customer' : 'New Customer'}
          </h1>
          <p style={S.pageSubtitle}>
            {isEdit ? 'Update customer details below' : 'Fill in the details to create a new customer'}
          </p>
        </div>
      </div>

      {/* ── Server error banner ───────────────────────────── */}
      {serverError && (
        <div style={S.errorBanner}>✕ {serverError}</div>
      )}

      <div style={S.formGrid}>

        {/* ════════════════════════════════════════════════
            SECTION 1 — Basic Information
        ════════════════════════════════════════════════ */}
        <section style={S.card}>
          <div style={S.cardHeader}>
            <span style={S.cardIcon}>👤</span>
            <div>
              <h2 style={S.cardTitle}>Basic Information</h2>
              <p style={S.cardSubtitle}>Required fields are marked with *</p>
            </div>
          </div>

          <div style={S.fieldGrid}>
            {/* Name */}
            <div style={S.fieldFull}>
              <label style={S.label}>Full Name *</label>
              <input
                style={{ ...S.input, ...(errors.name ? S.inputError : {}) }}
                type="text"
                placeholder="e.g. Ashan Perera"
                value={form.name}
                onChange={e => handleField('name', e.target.value)}
              />
              {errors.name && <span style={S.errMsg}>{errors.name}</span>}
            </div>

            {/* Date of Birth */}
            <div style={S.fieldHalf}>
              <label style={S.label}>Date of Birth *</label>
              <input
                style={{ ...S.input, ...(errors.dateOfBirth ? S.inputError : {}) }}
                type="date"
                value={form.dateOfBirth}
                max={new Date().toISOString().split('T')[0]}
                onChange={e => handleField('dateOfBirth', e.target.value)}
              />
              {errors.dateOfBirth && <span style={S.errMsg}>{errors.dateOfBirth}</span>}
            </div>

            {/* NIC Number */}
            <div style={S.fieldHalf}>
              <label style={S.label}>NIC Number *</label>
              <input
                style={{ ...S.input, ...(errors.nicNumber ? S.inputError : {}), fontFamily: 'monospace' }}
                type="text"
                placeholder="e.g. 900751234V"
                value={form.nicNumber}
                onChange={e => handleField('nicNumber', e.target.value.toUpperCase())}
              />
              {errors.nicNumber && <span style={S.errMsg}>{errors.nicNumber}</span>}
            </div>
          </div>
        </section>

        {/* ════════════════════════════════════════════════
            SECTION 2 — Mobile Numbers
        ════════════════════════════════════════════════ */}
        <section style={S.card}>
          <div style={S.cardHeader}>
            <span style={S.cardIcon}>📱</span>
            <div>
              <h2 style={S.cardTitle}>Mobile Numbers</h2>
              <p style={S.cardSubtitle}>Optional — add multiple numbers</p>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {form.mobileNumbers.map((num, i) => (
              <div key={i} style={S.repeatRow}>
                <input
                  style={{ ...S.input, flex: 1 }}
                  type="tel"
                  placeholder="+94771234567"
                  value={num}
                  onChange={e => updateMobile(i, e.target.value)}
                />
                {form.mobileNumbers.length > 1 && (
                  <button
                    style={S.removeBtn}
                    onClick={() => removeMobile(i)}
                    title="Remove"
                  >
                    ✕
                  </button>
                )}
              </div>
            ))}
            <button style={S.addBtn} onClick={addMobile}>
              + Add another number
            </button>
          </div>
        </section>

        {/* ════════════════════════════════════════════════
            SECTION 3 — Addresses
        ════════════════════════════════════════════════ */}
        <section style={S.card}>
          <div style={S.cardHeader}>
            <span style={S.cardIcon}>🏠</span>
            <div>
              <h2 style={S.cardTitle}>Addresses</h2>
              <p style={S.cardSubtitle}>Optional — add multiple addresses</p>
            </div>
          </div>

          {form.addresses.length === 0 && (
            <p style={S.emptyHint}>No addresses added yet.</p>
          )}

          {form.addresses.map((addr, i) => (
            <div key={i} style={S.addressBlock}>
              <div style={S.addressBlockHeader}>
                <span style={S.addressLabel}>Address {i + 1}</span>
                <button
                  style={S.removeBtn}
                  onClick={() => removeAddress(i)}
                >
                  ✕ Remove
                </button>
              </div>

              <div style={S.fieldGrid}>
                {/* Address Line 1 */}
                <div style={S.fieldFull}>
                  <label style={S.label}>Address Line 1 *</label>
                  <input
                    style={{
                      ...S.input,
                      ...(errors[`addr_${i}_line1`] ? S.inputError : {}),
                    }}
                    type="text"
                    placeholder="Street number and name"
                    value={addr.addressLine1}
                    onChange={e => updateAddress(i, 'addressLine1', e.target.value)}
                  />
                  {errors[`addr_${i}_line1`] && (
                    <span style={S.errMsg}>{errors[`addr_${i}_line1`]}</span>
                  )}
                </div>

                {/* Address Line 2 */}
                <div style={S.fieldFull}>
                  <label style={S.label}>Address Line 2</label>
                  <input
                    style={S.input}
                    type="text"
                    placeholder="Apartment, suburb, area (optional)"
                    value={addr.addressLine2}
                    onChange={e => updateAddress(i, 'addressLine2', e.target.value)}
                  />
                </div>

                {/* Country dropdown */}
                <div style={S.fieldHalf}>
                  <label style={S.label}>Country</label>
                  <select
                    style={S.select}
                    value={addr.countryId}
                    onChange={e => updateAddress(i, 'countryId', e.target.value)}
                  >
                    <option value="">— Select country —</option>
                    {countries.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>

                {/* City dropdown — populated after country is selected */}
                <div style={S.fieldHalf}>
                  <label style={S.label}>City</label>
                  <select
                    style={{
                      ...S.select,
                      opacity: addr.countryId ? 1 : 0.5,
                    }}
                    value={addr.cityId}
                    disabled={!addr.countryId}
                    onChange={e => updateAddress(i, 'cityId', e.target.value)}
                  >
                    <option value="">— Select city —</option>
                    {(addr.cities || []).map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                  {!addr.countryId && (
                    <span style={S.hintMsg}>Select a country first</span>
                  )}
                </div>
              </div>
            </div>
          ))}

          <button style={S.addBtn} onClick={addAddress}>
            + Add address
          </button>
        </section>

        {/* ════════════════════════════════════════════════
            SECTION 4 — Family Members
        ════════════════════════════════════════════════ */}
        <section style={S.card}>
          <div style={S.cardHeader}>
            <span style={S.cardIcon}>👨‍👩‍👧‍👦</span>
            <div>
              <h2 style={S.cardTitle}>Family Members</h2>
              <p style={S.cardSubtitle}>Link to existing customers in the system</p>
            </div>
          </div>

          {/* Search box */}
          <div style={{ position: 'relative', marginBottom: '16px' }}>
            <input
              style={S.input}
              type="text"
              placeholder="Search by name or NIC to find a customer..."
              value={familySearch}
              onChange={e => searchFamilyMembers(e.target.value)}
            />
            {/* Dropdown results */}
            {familyResults.length > 0 && (
              <div style={S.searchDropdown}>
                {familyResults.map(c => (
                  <button
                    key={c.id}
                    style={S.searchResultItem}
                    onClick={() => addFamilyMember(c)}
                  >
                    <div style={S.searchResultAvatar}>
                      {c.name.charAt(0)}
                    </div>
                    <div>
                      <div style={{ fontWeight: 500, fontSize: 14 }}>{c.name}</div>
                      <div style={{ fontSize: 12, color: '#64748b', fontFamily: 'monospace' }}>
                        {c.nicNumber}
                      </div>
                    </div>
                    <span style={S.addTag}>+ Add</span>
                  </button>
                ))}
              </div>
            )}
            {searchingFamily && (
              <div style={{ ...S.searchDropdown, padding: '12px 16px', color: '#64748b', fontSize: 13 }}>
                Searching...
              </div>
            )}
          </div>

          {/* Added family members list */}
          {form.familyMembers.length === 0 ? (
            <p style={S.emptyHint}>No family members linked yet.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {form.familyMembers.map(member => (
                <div key={member.id} style={S.familyMemberRow}>
                  <div style={S.searchResultAvatar}>{member.name?.charAt(0)}</div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 500, fontSize: 14 }}>{member.name}</div>
                    <div style={{ fontSize: 12, color: '#64748b', fontFamily: 'monospace' }}>
                      {member.nicNumber}
                    </div>
                  </div>
                  <button
                    style={S.removeBtn}
                    onClick={() => removeFamilyMember(member.id)}
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

      </div>{/* end formGrid */}

      {/* ── Submit bar (sticky at bottom) ─────────────────── */}
      <div style={S.submitBar}>
        <button
          style={S.cancelBtn}
          onClick={() => onNavigate('list')}
          disabled={submitting}
        >
          Cancel
        </button>
        <button
          style={{ ...S.submitBtn, opacity: submitting ? 0.7 : 1 }}
          onClick={handleSubmit}
          disabled={submitting}
        >
          {submitting
            ? (isEdit ? 'Saving...' : 'Creating...')
            : (isEdit ? 'Save Changes' : 'Create Customer')
          }
        </button>
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
    maxWidth: '860px',
    margin: '0 auto',
    fontFamily: "'DM Sans', sans-serif",
    paddingBottom: '100px',   // space for sticky submit bar
  },
  pageHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '20px',
    marginBottom: '28px',
  },
  backBtn: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    color: '#475569',
    padding: '8px 16px',
    borderRadius: '8px',
    cursor: 'pointer',
    fontSize: '14px',
    whiteSpace: 'nowrap',
  },
  pageTitle: {
    fontSize: '26px',
    fontWeight: '700',
    color: '#0f172a',
    margin: '0 0 4px 0',
  },
  pageSubtitle: {
    fontSize: '14px',
    color: '#64748b',
    margin: 0,
  },
  errorBanner: {
    background: '#fef2f2',
    border: '1px solid #fca5a5',
    color: '#991b1b',
    padding: '12px 16px',
    borderRadius: '8px',
    marginBottom: '20px',
    fontSize: '14px',
  },
  formGrid: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
  },
  card: {
    background: '#fff',
    border: '1px solid #e2e8f0',
    borderRadius: '14px',
    padding: '24px',
  },
  cardHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '14px',
    marginBottom: '24px',
    paddingBottom: '16px',
    borderBottom: '1px solid #f1f5f9',
  },
  cardIcon: {
    fontSize: '28px',
    lineHeight: 1,
  },
  cardTitle: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#0f172a',
    margin: '0 0 2px 0',
  },
  cardSubtitle: {
    fontSize: '13px',
    color: '#94a3b8',
    margin: 0,
  },
  fieldGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '16px',
  },
  fieldFull: {
    gridColumn: '1 / -1',
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  fieldHalf: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  label: {
    fontSize: '13px',
    fontWeight: '500',
    color: '#374151',
  },
  input: {
    padding: '10px 14px',
    border: '1px solid #e2e8f0',
    borderRadius: '8px',
    fontSize: '14px',
    color: '#0f172a',
    outline: 'none',
    width: '100%',
    boxSizing: 'border-box',
    transition: 'border-color 0.2s',
    backgroundColor: '#fff',
  },
  inputError: {
    borderColor: '#ef4444',
    background: '#fef2f2',
  },
  select: {
    padding: '10px 14px',
    border: '1px solid #e2e8f0',
    borderRadius: '8px',
    fontSize: '14px',
    color: '#0f172a',
    outline: 'none',
    width: '100%',
    boxSizing: 'border-box',
    backgroundColor: '#fff',
    cursor: 'pointer',
    appearance: 'auto',
  },
  errMsg: {
    fontSize: '12px',
    color: '#ef4444',
  },
  hintMsg: {
    fontSize: '12px',
    color: '#94a3b8',
  },
  repeatRow: {
    display: 'flex',
    gap: '10px',
    alignItems: 'center',
  },
  addBtn: {
    marginTop: '10px',
    background: 'none',
    border: '1px dashed #cbd5e1',
    color: '#3b82f6',
    padding: '10px 16px',
    borderRadius: '8px',
    fontSize: '13px',
    cursor: 'pointer',
    width: '100%',
    textAlign: 'center',
    transition: 'all 0.15s',
  },
  removeBtn: {
    background: '#fef2f2',
    border: '1px solid #fecaca',
    color: '#ef4444',
    padding: '6px 12px',
    borderRadius: '6px',
    fontSize: '12px',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  emptyHint: {
    fontSize: '14px',
    color: '#94a3b8',
    textAlign: 'center',
    padding: '16px 0',
    margin: 0,
  },
  addressBlock: {
    border: '1px solid #f1f5f9',
    borderRadius: '10px',
    padding: '16px',
    marginBottom: '12px',
    background: '#f8fafc',
  },
  addressBlockHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '14px',
  },
  addressLabel: {
    fontSize: '13px',
    fontWeight: '600',
    color: '#475569',
  },
  searchDropdown: {
    position: 'absolute',
    top: '100%',
    left: 0,
    right: 0,
    background: '#fff',
    border: '1px solid #e2e8f0',
    borderRadius: '10px',
    boxShadow: '0 8px 24px rgba(0,0,0,0.1)',
    zIndex: 200,
    overflow: 'hidden',
    marginTop: '4px',
  },
  searchResultItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px 16px',
    width: '100%',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    textAlign: 'left',
    transition: 'background 0.1s',
    borderBottom: '1px solid #f1f5f9',
  },
  searchResultAvatar: {
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
  addTag: {
    marginLeft: 'auto',
    fontSize: '12px',
    color: '#3b82f6',
    background: '#eff6ff',
    padding: '3px 10px',
    borderRadius: '20px',
    fontWeight: 500,
  },
  familyMemberRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '10px 14px',
    background: '#f8fafc',
    borderRadius: '8px',
    border: '1px solid #e2e8f0',
  },
  submitBar: {
    position: 'fixed',
    bottom: 0,
    left: 0,
    right: 0,
    background: '#fff',
    borderTop: '1px solid #e2e8f0',
    padding: '16px 32px',
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '12px',
    zIndex: 100,
    boxShadow: '0 -4px 16px rgba(0,0,0,0.06)',
  },
  cancelBtn: {
    background: '#fff',
    border: '1px solid #d1d5db',
    color: '#374151',
    padding: '10px 24px',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '500',
    cursor: 'pointer',
  },
  submitBtn: {
    background: '#3b82f6',
    color: '#fff',
    border: 'none',
    padding: '10px 28px',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'opacity 0.2s',
  },
  loadingWrap: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '300px',
  },
  spinner: {
    width: '36px',
    height: '36px',
    border: '3px solid #e2e8f0',
    borderTop: '3px solid #3b82f6',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
};