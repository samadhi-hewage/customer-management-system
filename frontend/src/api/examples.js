// ============================================================
//  src/api/examples.js
//  Copy-paste examples of how to use the API in your components.
//  You do NOT need to add this file to your project —
//  it is a reference guide only.
// ============================================================

import { customerApi, bulkApi, masterApi } from './axios';

// ============================================================
//  EXAMPLE 1 — Load all customers into a table
// ============================================================
async function loadCustomers() {
  try {
    const response = await customerApi.getAll('', 0, 20);
    const { content, totalElements, totalPages } = response.data;
    console.log('Customers:', content);
    console.log('Total:', totalElements);
  } catch (error) {
    console.error('Failed to load customers:', error.response?.data?.message);
  }
}

// ============================================================
//  EXAMPLE 2 — Search customers by name or NIC
// ============================================================
async function searchCustomers(searchTerm) {
  try {
    const response = await customerApi.getAll(searchTerm, 0, 20);
    return response.data.content;
  } catch (error) {
    console.error('Search failed:', error);
  }
}

// ============================================================
//  EXAMPLE 3 — Create a new customer
// ============================================================
async function createCustomer() {
  const customerData = {
    name: 'Ashan Perera',
    dateOfBirth: '1990-03-15',        // must be yyyy-MM-dd
    nicNumber: '900751234V',
    mobileNumbers: ['+94771234567', '+94711234567'],
    addresses: [
      {
        addressLine1: '45 Galle Road',
        addressLine2: 'Kollupitiya',
        cityId: 1,
        countryId: 1,
      }
    ],
    familyMembers: [
      { id: 2 }                       // ID of an existing customer
    ],
  };

  try {
    const response = await customerApi.create(customerData);
    console.log('Created customer ID:', response.data.id);
    return response.data;
  } catch (error) {
    if (error.response?.status === 400) {
      // Validation errors — show to user
      const { fields, message } = error.response.data;
      if (fields) {
        Object.entries(fields).forEach(([field, msg]) => {
          console.error(`${field}: ${msg}`);
        });
      } else {
        console.error(message);         // e.g. "NIC number already exists"
      }
    }
  }
}

// ============================================================
//  EXAMPLE 4 — Update an existing customer
// ============================================================
async function updateCustomer(id) {
  try {
    const response = await customerApi.update(id, {
      name: 'Ashan Perera Updated',
      dateOfBirth: '1990-03-15',
      nicNumber: '900751234V',
      mobileNumbers: ['+94779999999'],
      addresses: [],
      familyMembers: [],
    });
    console.log('Updated:', response.data);
  } catch (error) {
    console.error('Update failed:', error.response?.data);
  }
}

// ============================================================
//  EXAMPLE 5 — Delete a customer
// ============================================================
async function deleteCustomer(id) {
  try {
    await customerApi.delete(id);
    console.log('Deleted successfully');
  } catch (error) {
    console.error('Delete failed:', error.response?.data?.message);
  }
}

// ============================================================
//  EXAMPLE 6 — Upload Excel file with progress tracking
// ============================================================
async function uploadExcel(file) {
  try {
    // Step 1: Upload the file — returns immediately with a jobId
    const uploadResponse = await bulkApi.upload(file, (progressEvent) => {
      const percent = Math.round(
        (progressEvent.loaded * 100) / progressEvent.total
      );
      console.log(`Upload progress: ${percent}%`);  // use this for a progress bar
    });

    const { jobId } = uploadResponse.data;
    console.log('Job started:', jobId);

    // Step 2: Poll for processing status every 2 seconds
    const interval = setInterval(async () => {
      try {
        const statusResponse = await bulkApi.getStatus(jobId);
        const { status, processed, totalRows, failed, percent } = statusResponse.data;

        console.log(`Status: ${status} | ${processed}/${totalRows} rows | ${percent}%`);

        if (status === 'DONE' || status === 'FAILED') {
          clearInterval(interval);      // stop polling
          console.log('Job finished:', statusResponse.data);
        }
      } catch (err) {
        clearInterval(interval);
        console.error('Status check failed:', err);
      }
    }, 2000);

  } catch (error) {
    console.error('Upload failed:', error.response?.data);
  }
}

// ============================================================
//  EXAMPLE 7 — Load countries then cities for a selected country
// ============================================================
async function loadAddressDropdowns(selectedCountryId) {
  try {
    // Load all countries (on page mount)
    const countriesRes = await masterApi.getCountries();
    const countries = countriesRes.data;
    console.log('Countries:', countries);

    // Load cities when user picks a country
    if (selectedCountryId) {
      const citiesRes = await masterApi.getCitiesByCountry(selectedCountryId);
      const cities = citiesRes.data;
      console.log('Cities for country', selectedCountryId, ':', cities);
    }
  } catch (error) {
    console.error('Failed to load master data:', error);
  }
}