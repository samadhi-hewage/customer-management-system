# Customer Management System

A full-stack web application for managing customers, built with Spring Boot and React.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Running the Backend](#running-the-backend)
- [Running the Frontend](#running-the-frontend)
- [Running the Tests](#running-the-tests)
- [API Reference](#api-reference)
- [Bulk Upload Guide](#bulk-upload-guide)
- [Features](#features)

---

## Tech Stack

| Layer     | Technology                        |
|-----------|-----------------------------------|
| Backend   | Java 17, Spring Boot 3.5, Maven   |
| Frontend  | React 18, Axios                   |
| Database  | MariaDB 10.x                      |
| Testing   | JUnit 5, Mockito, H2 (test DB)    |
| Excel     | Apache POI 5.2.5 (SAX streaming)  |

---

## Project Structure

```
customer-management-system/
├── backend/                          # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/customermanagement/
│   │   │   │   ├── config/
│   │   │   │   │   ├── AsyncConfig.java
│   │   │   │   │   └── CorsConfig.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── CustomerController.java
│   │   │   │   │   ├── BulkUploadController.java
│   │   │   │   │   ├── MasterDataController.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   ├── dto/
│   │   │   │   │   └── CustomerDTO.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Customer.java
│   │   │   │   │   ├── Address.java
│   │   │   │   │   ├── MobileNumber.java
│   │   │   │   │   ├── City.java
│   │   │   │   │   ├── Country.java
│   │   │   │   │   └── BulkJob.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── CustomerRepository.java
│   │   │   │   │   ├── AddressRepository.java
│   │   │   │   │   ├── MobileNumberRepository.java
│   │   │   │   │   ├── CityRepository.java
│   │   │   │   │   ├── CountryRepository.java
│   │   │   │   │   └── BulkJobRepository.java
│   │   │   │   └── service/
│   │   │   │       ├── CustomerService.java
│   │   │   │       └── BulkUploadService.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       ├── java/com/example/customermanagement/
│   │       │   ├── service/
│   │       │   │   ├── CustomerServiceTest.java
│   │       │   │   └── BulkUploadServiceTest.java
│   │       │   └── repository/
│   │       │       └── CustomerRepositoryTest.java
│   │       └── resources/
│   │           └── application-test.properties
│   └── pom.xml
├── frontend/                         # React application
│   ├── src/
│   │   ├── api/
│   │   │   └── axios.js
│   │   ├── components/
│   │   │   ├── CustomerList.jsx
│   │   │   ├── CustomerForm.jsx
│   │   │   └── BulkUpload.jsx
│   │   └── App.jsx
│   └── package.json
├
│  
└── README.md
```

---

## Prerequisites

Make sure the following are installed before running the project.

| Tool        | Version  | Download                          |
|-------------|----------|-----------------------------------|
| Java JDK    | 17+      | https://adoptium.net              |
| Maven       | 3.8+     | https://maven.apache.org          |
| Node.js     | 18+      | https://nodejs.org                |
| MariaDB     | 10.6+    | https://mariadb.org/download      |

Verify installations:

```bash
java -version
mvn -version
node -version
npm -version
```

---

## Database Setup

### Step 1 — Create the database

Open a MariaDB client (DBeaver, HeidiSQL, or terminal) and run:

```sql
CREATE DATABASE customer_db;
USE customer_db;
```

### Step 2 — Run the DDL script

Run the full contents of `db/ddl_customer_management.sql`.

This creates 7 tables:

| Table            | Purpose                                  |
|------------------|------------------------------------------|
| `customers`      | Core customer records                    |
| `mobile_numbers` | Multiple phone numbers per customer      |
| `addresses`      | Multiple addresses per customer          |
| `family_members` | Self-referencing links between customers |
| `cities`         | Master data — city list                  |
| `countries`      | Master data — country list               |
| `bulk_jobs`      | Tracks async Excel upload jobs           |

### Step 3 — Run the DML seed script

Run the full contents of `db/dml_seed_data.sql`.

This inserts:
- 10 countries (Sri Lanka, US, UK, Australia, India, Canada, Germany, France, Singapore, UAE)
- 39 cities across those countries
- 5 sample customers with addresses, mobile numbers and family links

### Step 4 — Verify

```sql
SELECT 'countries'     AS tbl, COUNT(*) AS rows FROM countries
UNION ALL SELECT 'cities',      COUNT(*) FROM cities
UNION ALL SELECT 'customers',   COUNT(*) FROM customers;
```

Expected output: countries=10, cities=39, customers=5.

---

## Running the Backend

### Step 1 — Configure database credentials

Open `backend/src/main/resources/application.properties` and update:

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/customer_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE
```

### Step 2 — Start the application

```bash
cd backend
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**.

You should see:

```
Started CustomerManagementApplication in 3.2 seconds
```

### Step 3 — Verify the backend is running

Open your browser and visit:

```
http://localhost:8080/api/master/countries
```

You should see a JSON array of 10 countries.

---

## Running the Frontend

Open a **new terminal** (keep the backend running in the first one).

### Step 1 — Install dependencies

```bash
cd frontend
npm install
```

### Step 2 — Start the React app

```bash
npm start
```

The frontend starts on **http://localhost:3000** and opens automatically in your browser.

> The frontend expects the backend to be running on port 8080. Make sure Spring Boot is started before using the UI.

---

## Running the Tests

```bash
cd backend

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=CustomerServiceTest
mvn test -Dtest=CustomerRepositoryTest
mvn test -Dtest=BulkUploadServiceTest
```

### Test summary

| Test Class                  | Tests | Description                                      |
|-----------------------------|-------|--------------------------------------------------|
| `CustomerServiceTest`       | 16    | Business logic using Mockito mocks               |
| `CustomerRepositoryTest`    | 18    | JPA queries against H2 in-memory database        |
| `BulkUploadServiceTest`     | 14    | Excel streaming, batch insert, job tracking      |
| **Total**                   | **48**|                                                  |

> Tests use an H2 in-memory database — no MariaDB connection required to run tests.

Expected output:

```
[INFO] Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## API Reference

### Customer endpoints

| Method   | URL                          | Description                          |
|----------|------------------------------|--------------------------------------|
| `GET`    | `/api/customers`             | List all customers (paginated)       |
| `GET`    | `/api/customers?search=&page=0&size=20` | Search by name or NIC   |
| `GET`    | `/api/customers/{id}`        | Get one customer with full details   |
| `POST`   | `/api/customers`             | Create a new customer                |
| `PUT`    | `/api/customers/{id}`        | Update an existing customer          |
| `DELETE` | `/api/customers/{id}`        | Delete a customer                    |

### Bulk upload endpoints

| Method | URL                          | Description                          |
|--------|------------------------------|--------------------------------------|
| `POST` | `/api/bulk/upload`           | Upload Excel file (multipart/form-data) |
| `GET`  | `/api/bulk/status/{jobId}`   | Poll processing progress             |

### Master data endpoints

| Method | URL                              | Description                      |
|--------|----------------------------------|----------------------------------|
| `GET`  | `/api/master/countries`          | All countries (for dropdown)     |
| `GET`  | `/api/master/cities?countryId=1` | Cities for a country (dropdown)  |

### Example — Create a customer

**Request:**
```http
POST /api/customers
Content-Type: application/json

{
  "name": "Ashan Perera",
  "dateOfBirth": "1990-03-15",
  "nicNumber": "900751234V",
  "mobileNumbers": ["+94771234567", "+94711234567"],
  "addresses": [
    {
      "addressLine1": "45 Galle Road",
      "addressLine2": "Kollupitiya",
      "cityId": 1,
      "countryId": 1
    }
  ],
  "familyMembers": []
}
```

**Response (201 Created):**
```json
{
  "id": 6,
  "name": "Ashan Perera",
  "dateOfBirth": "1990-03-15",
  "nicNumber": "900751234V",
  "mobileNumbers": ["+94771234567", "+94711234567"],
  "addresses": [
    {
      "id": 1,
      "addressLine1": "45 Galle Road",
      "addressLine2": "Kollupitiya",
      "cityId": 1,
      "cityName": "Colombo",
      "countryId": 1,
      "countryName": "Sri Lanka"
    }
  ],
  "familyMembers": []
}
```

---

## Bulk Upload Guide

### Excel file format

The upload file must be `.xlsx` or `.xls` with exactly these columns in order:

| Column | Header          | Format       | Required |
|--------|-----------------|--------------|----------|
| A      | `Name`          | Any text     | Yes      |
| B      | `Date of Birth` | `yyyy-MM-dd` | Yes      |
| C      | `NIC Number`    | Unique value | Yes      |

Example file contents:

```
Name              | Date of Birth | NIC Number
Ashan Perera      | 1990-03-15    | 900751234V
Dilani Silva      | 1985-07-22    | 857031456V
```

### How to upload

**Using the UI:**
1. Click **Bulk Upload** in the navigation bar
2. Drag and drop your `.xlsx` file or click to browse
3. Click **Upload and Process**
4. Watch the progress bar update in real time
5. When complete, click **View customers** to see the results

**Using Postman:**
1. `POST http://localhost:8080/api/bulk/upload`
2. Body → form-data → Key: `file`, Type: File → select your `.xlsx`
3. Copy the `jobId` from the response
4. Poll `GET http://localhost:8080/api/bulk/status/{jobId}` every few seconds

### How large files are handled

Files with up to 1,000,000 rows are supported without timeout or memory issues because:

- The backend uses a **SAX (event-based) XML parser** that reads one row at a time — memory usage stays constant regardless of file size
- Rows are inserted in **batches of 500** using `JdbcTemplate.batchUpdate()` — one database round trip per 500 rows instead of one per row
- Processing runs in a **background thread** (`@Async`) so the HTTP request returns immediately with a `jobId`
- If a NIC already exists, the row is **updated** rather than inserted
- Rows missing mandatory fields are **skipped** and counted as failed

### Status response

```json
{
  "jobId": "a1b2c3d4-e5f6-...",
  "status": "PROCESSING",
  "fileName": "customers.xlsx",
  "totalRows": 10000,
  "processed": 5000,
  "failed": 12,
  "percent": 50
}
```

Status values: `PENDING` → `PROCESSING` → `DONE` or `FAILED`

---

## Features

- Create, view, update and delete customers
- Mandatory fields: Name, Date of Birth, NIC Number (unique)
- Multiple mobile numbers per customer (optional)
- Multiple addresses per customer with city and country dropdowns (optional)
- Family member linking — customers can be linked to other customers
- Paginated customer list with search by name or NIC
- Bulk create and update via Excel upload (supports 1,000,000+ rows)
- Real-time upload progress tracking
- Input validation with clear error messages
- Cities and countries stored as master data in the database



## Screenshots

![Screenshot 1](screenshots/Screenshot%202026-05-01%20060414.png)

![Screenshot 2](screenshots/Screenshot%202026-05-01%20060315.png)

![Screenshot 3](screenshots/Screenshot%202026-05-01%20060330.png)

