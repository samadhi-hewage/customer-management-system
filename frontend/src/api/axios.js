// ============================================================
//  src/api/axios.js
//  Central Axios configuration for the entire React app.
//  Import { customerApi, bulkApi, masterApi } in your components.
// ============================================================

import axios from 'axios';

// -------------------------------------------------------
// BASE INSTANCE
// Every API call uses this instance automatically —
// no need to repeat the base URL or headers anywhere.
// -------------------------------------------------------
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,                          // 30 seconds — fail if no response
  headers: {
    'Content-Type': 'application/json',
  },
});

// -------------------------------------------------------
// REQUEST INTERCEPTOR
// Runs before every request is sent.
// Good place to attach auth tokens later if needed.
// -------------------------------------------------------
api.interceptors.request.use(
  (config) => {
    // Log every request in development so you can debug easily
    if (process.env.NODE_ENV === 'development') {
      console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`, config.data || '');
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// -------------------------------------------------------
// RESPONSE INTERCEPTOR
// Runs after every response comes back.
// Centralises error handling — components don't need
// to handle network errors individually.
// -------------------------------------------------------
api.interceptors.response.use(
  (response) => {
    // Successful response — just return the data
    return response;
  },
  (error) => {
    if (error.response) {
      // Server responded with an error status (4xx, 5xx)
      const { status, data } = error.response;

      switch (status) {
        case 400:
          // Validation error from @Valid — data.fields has field-level messages
          console.error('[API] Validation error:', data);
          break;
        case 404:
          console.error('[API] Not found:', error.config?.url);
          break;
        case 500:
          console.error('[API] Server error:', data?.message);
          break;
        default:
          console.error('[API] Error:', status, data);
      }
    } else if (error.code === 'ECONNABORTED') {
      // Request timed out
      console.error('[API] Request timed out');
    } else if (!error.response) {
      // No response at all — Spring Boot not running
      console.error('[API] Cannot reach server — is Spring Boot running on port 8080?');
    }

    // Always re-throw so components can handle errors in their own catch blocks
    return Promise.reject(error);
  }
);

// ============================================================
//  CUSTOMER API
//  All endpoints for creating, updating, viewing customers
// ============================================================
export const customerApi = {

  // GET /api/customers?search=&page=0&size=20
  // Returns paginated list of customers
  getAll: (search = '', page = 0, size = 20) =>
    api.get('/customers', {
      params: { search, page, size },
    }),

  // GET /api/customers/{id}
  // Returns full customer detail including mobiles, addresses, family
  getById: (id) =>
    api.get(`/customers/${id}`),

  // POST /api/customers
  // Body: CustomerDTO { name, dateOfBirth, nicNumber, mobileNumbers[], addresses[], familyMembers[] }
  create: (customerData) =>
    api.post('/customers', customerData),

  // PUT /api/customers/{id}
  // Body: same as create
  update: (id, customerData) =>
    api.put(`/customers/${id}`, customerData),

  // DELETE /api/customers/{id}
  // Returns 204 No Content on success
  delete: (id) =>
    api.delete(`/customers/${id}`),
};

// ============================================================
//  BULK UPLOAD API
//  Endpoints for Excel file upload and job status polling
// ============================================================
export const bulkApi = {

  // POST /api/bulk/upload
  // Sends the Excel file as multipart/form-data
  // Returns { jobId, status: "PENDING", message }
  upload: (file, onUploadProgress) => {
    const formData = new FormData();
    formData.append('file', file);

    return api.post('/bulk/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',  // override JSON default for file upload
      },
      timeout: 60000,                            // 60 seconds for large file upload itself
      onUploadProgress,                          // callback for upload progress bar
    });
  },

  // GET /api/bulk/status/{jobId}
  // Returns { jobId, status, totalRows, processed, failed, percent, errorMsg }
  getStatus: (jobId) =>
    api.get(`/bulk/status/${jobId}`),
};

// ============================================================
//  MASTER DATA API
//  Countries and cities for address dropdowns
// ============================================================
export const masterApi = {

  // GET /api/master/countries
  // Returns all countries sorted A-Z
  getCountries: () =>
    api.get('/master/countries'),

  // GET /api/master/cities?countryId=1
  // Returns cities for a specific country
  getCitiesByCountry: (countryId) =>
    api.get('/master/cities', {
      params: { countryId },
    }),
};

// Export the raw instance too in case you need it directly
export default api;