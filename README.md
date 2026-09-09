# JSON Dataset API

A small, focused Spring Boot REST API that stores JSON records in PostgreSQL and lets you query them dynamically — **group by any field**, **sort by any field** — without writing a single line of database-specific code for each query.

The idea is simple: you can drop any JSON object into a named "dataset", and then ask the API to group or sort those records by whatever field makes sense for your data. Because the fields are passed as query parameters, the same API works for an employee dataset, a product catalog, a transactions log — anything that is essentially a list of JSON objects.

> **🚀 Live API:** [https://json-dataset-api.onrender.com](https://json-dataset-api.onrender.com) — deployed on Render (Docker) with a Neon PostgreSQL database. You can call it right now, no setup needed.

## Why This Exists

Most CRUD backends hardcode a fixed schema. If you suddenly need to group a list of people by their city instead of their department, you have to change code. This project avoids that friction: the dataset stores records as **JSONB**, and grouping/sorting happens on the fly against the JSON itself. No schema change, no code change — just a different query parameter.

## What You Can Do With It

- Save any JSON object into a named dataset (`POST`)
- Group all records by any field — `?groupBy=department` or `?groupBy=city` (your choice)
- Sort all records by any field, ascending or descending — `?sortBy=age&order=desc`
- All of the above works on any dataset you create, no recompilation required

## Technologies Used

- **Java 21**
- **Spring Boot 3.3**
- **Spring Web** — REST endpoints
- **Spring Data JPA** — database access
- **PostgreSQL** — relational database with `JSONB` column
- **Maven** — build tool
- **JUnit 5 / Mockito** — testing
- **Docker** — containerized deployment (see `Dockerfile` + `render.yaml`)

## Project Structure

```
src/main/java/com/example/jsondataset/
├── controller/   DatasetController      (REST endpoints)
├── service/      DatasetService         (insert, group-by, sort-by logic)
├── repository/   DatasetRepository      (JPA data access)
├── entity/       DatasetRecord          (the JSONB row)
├── dto/          Insert/GroupBy/SortBy responses
└── exception/    custom exceptions + global handler
```

## Architecture

```
Controller (REST endpoints)
        |
        v
Service (business logic: insert, group-by, sort-by)
        |
        v
Repository (JPA)
        |
        v
PostgreSQL (JSONB storage)
```

Records are saved to a single `dataset_records` table. Each row stores the dataset name and the full JSON record in a `jsonb` column. Group-by and sort-by are performed in the service layer using Jackson.

## Database Design

| Column        | Type        | Description                          |
|---------------|-------------|--------------------------------------|
| `id`          | BIGSERIAL   | Primary key (auto-generated)         |
| `dataset_name`| VARCHAR(255)| Name of the dataset                  |
| `record_data` | JSONB       | The JSON record                     |
| `created_at`  | TIMESTAMP   | Record creation time                 |

The table is created automatically by Hibernate (`ddl-auto=update`). No manual SQL setup is required.

## API Endpoints

### 1. Insert Record

```
POST /api/dataset/{datasetName}/record
```

**Request body** (any JSON object):

```json
{
  "id": 1,
  "name": "John Doe",
  "age": 30,
  "department": "Engineering"
}
```

**Response** `201 Created`:

```json
{
  "message": "Record added successfully",
  "dataset": "employee_dataset",
  "recordId": 1
}
```

### 2. Query with Group-By

```
GET /api/dataset/{datasetName}/query?groupBy={fieldName}
```

**Example:**

```
GET /api/dataset/employee_dataset/query?groupBy=department
```

**Response** `200 OK`:

```json
{
  "groupedRecords": {
    "Engineering": [
      { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" },
      { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" }
    ],
    "Marketing": [
      { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" }
    ]
  }
}
```

### 3. Query with Sort-By

```
GET /api/dataset/{datasetName}/query?sortBy={fieldName}&order={asc|desc}
```

**Example:**

```
GET /api/dataset/employee_dataset/query?sortBy=age&order=asc
```

**Response** `200 OK`:

```json
{
  "sortedRecords": [
    { "id": 2, "name": "Jane Smith", "age": 25, "department": "Engineering" },
    { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" },
    { "id": 1, "name": "John Doe", "age": 30, "department": "Engineering" }
  ]
}
```

`order` defaults to `asc`. Supported values: `asc`, `desc`.

## Error Responses

All errors return a consistent JSON structure with an appropriate HTTP status code:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Dataset 'ghost' does not exist",
  "timestamp": 1729999999999
}
```

| Scenario                              | HTTP Status |
|---------------------------------------|-------------|
| Dataset does not exist                | 404         |
| Dataset exists but has no records     | 404         |
| `groupBy`/`sortBy` field not found in records | 400   |
| Invalid `order` value                 | 400         |
| Missing `groupBy` and `sortBy` parameters | 400      |
| Empty or malformed JSON body          | 400         |
| Any unexpected error                  | 500         |

## Environment Variables

The application is configured entirely through environment variables — no credentials are hardcoded.

| Variable            | Required | Description                                    |
|---------------------|----------|------------------------------------------------|
| `DATABASE_URL`      | Yes      | JDBC URL of the PostgreSQL database            |
| `DATABASE_USERNAME` | Yes      | Database username                              |
| `DATABASE_PASSWORD` | Yes      | Database password                              |
| `PORT`              | No       | Server port (defaults to `8080`)               |

Example JDBC URL: `jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require`

## Running Locally

1. **Set up a PostgreSQL database.** Use a cloud-hosted database such as [Neon](https://neon.tech) (free tier) so your local setup matches the deployed environment.

2. **Configure environment variables** for your shell:

   ```
   DATABASE_URL=jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require
   DATABASE_USERNAME=<username>
   DATABASE_PASSWORD=<password>
   ```

3. **Build and run:**

   ```bash
   mvn clean package
   java -jar target/json-dataset-api-1.0.0.jar
   ```

   The app starts on `http://localhost:8080`.

## Running Tests

```bash
mvn test
```

The test suite covers service-level and controller-level behavior:

- Record insertion success and empty-body failure
- Group-by with valid and missing fields
- Sort ascending and descending (numeric and string fields)
- Invalid sort order
- Non-existent dataset, empty dataset
- Malformed JSON request

## Testing with Postman

### 1. Insert a record

- **Method:** `POST`
- **URL:** `https://json-dataset-api.onrender.com/api/dataset/employee_dataset/record`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**

```json
{
  "id": 1,
  "name": "John Doe",
  "age": 30,
  "department": "Engineering"
}
```

Add a few more records with different `department`, `age`, `name` values.

### 2. Query with group-by

- **Method:** `GET`
- **URL:** `https://json-dataset-api.onrender.com/api/dataset/employee_dataset/query?groupBy=department`

### 3. Query with sort-by

- **Method:** `GET`
- **URL:** `https://json-dataset-api.onrender.com/api/dataset/employee_dataset/query?sortBy=age&order=asc`

Try `order=desc`, or sort by another field such as `name`.

## Deploying to Render (with Neon PostgreSQL)

### 1. Create a PostgreSQL database on Neon

1. Sign up at [neon.tech](https://neon.tech).
2. Create a new project and database.
3. Copy the connection details (username, password, host, port, database name).

### 2. Deploy the application on Render

This project ships with a **`Dockerfile`** and a **`render.yaml`** blueprint, so deployment is automatic and reproducible.

1. Push this repository to a public GitHub repository.
2. Sign up at [render.com](https://render.com) and connect your GitHub account.
3. Click **New → Blueprint** and select this repository (Render reads the `render.yaml` file).
4. When prompted, set these **environment variables** (leave the values blank — Render will ask you for them):

   | Key                  | Value                                  |
   |----------------------|----------------------------------------|
   | `DATABASE_URL`       | `jdbc:postgresql://<host>:<port>/<db>` |
   | `DATABASE_USERNAME`  | `<your-neon-username>`                 |
   | `DATABASE_PASSWORD`  | `<your-neon-password>`                 |

5. Click **Apply**. Render builds the container from the `Dockerfile` and deploys your app.
6. The app is now live at `https://<your-app>.onrender.com` (for this project: `https://json-dataset-api.onrender.com`).

Render sets the `PORT` environment variable automatically — no configuration needed. The multi-stage `Dockerfile` compiles the project with Maven and runs the resulting JAR on a Java 21 runtime.

### 3. Verify

- Insert a record (see Postman instructions above).
- Query it back with group-by and sort-by.
- The data persists across redeploys because it lives in Neon, not in your app.
- The API works even when your computer is off.

## Live API

The application is deployed and running at **https://json-dataset-api.onrender.com** (Render + Neon PostgreSQL). You can call it directly — a recruiter needs no local setup:

### Live test

Insert a record:

```bash
curl -X POST https://json-dataset-api.onrender.com/api/dataset/employee_dataset/record \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"John Doe","age":30,"department":"Engineering"}'
```

Group by department:

```bash
curl "https://json-dataset-api.onrender.com/api/dataset/employee_dataset/query?groupBy=department"
```

Sort by age ascending:

```bash
curl "https://json-dataset-api.onrender.com/api/dataset/employee_dataset/query?sortBy=age&order=asc"
```

> **Free-tier note:** Render's free plan puts the server to sleep after ~15 minutes of inactivity. The first request after a sleep may take 30–60 seconds to "cold start" while it wakes up, then it responds normally.

## A Note on Scope

This is intentionally a focused assignment project — not a full product. There's no user auth, no admin dashboard, no caching, and no frontend. It does exactly what the assignment asks: persist JSON records, group them, sort them, and expose everything through clean REST endpoints backed by tests and clear documentation. Kept simple on purpose, so the code stays easy to read and review.
