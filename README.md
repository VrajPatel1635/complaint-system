# Smart Complaint System

Live site: https://complaint-system.up.railway.app/

A Spring Boot web application for registering and tracking complaints, with separate dashboards for Citizens and Admins.

## Features

### Citizen
- Register and log in (form-based authentication)
- Submit a complaint (title, description, location, category)
- View all your complaints
- View complaint details with a status/update timeline

### Admin
- Admin dashboard with complaint counts by status
- Manage complaints with pagination + filters (category + status)
- View complaint details with full timeline
- Update complaint status with remarks
- Reports page with charts (status distribution, category breakdown, trends)
- Export a date-range report to Excel (`.xlsx`)

## Complaint statuses

`PENDING` → `IN_PROGRESS` → `RESOLVED` / `REJECTED`

Notes:
- The service prevents skipping directly from `PENDING` to `RESOLVED`.
- Once `RESOLVED` or `REJECTED`, the complaint is considered closed (no further updates allowed).

## Tech stack

- Java 21
- Spring Boot 3 (MVC + Security + Data JPA)
- JSP + JSTL views (server-side rendering)
- MySQL
- Bootstrap 5 (CDN) + custom CSS/JS
- Apache POI (Excel export)

## Project structure

```
.
├─ pom.xml
├─ mvnw
├─ mvnw.cmd
└─ src/
   ├─ main/
   │  ├─ java/
   │  │  └─ com/project/complaintsystem/
   │  │     ├─ config/        # Security + MVC config + role seeding
   │  │     ├─ controller/    # MVC controllers (auth, user, admin)
   │  │     ├─ dto/
   │  │     ├─ enums/
   │  │     ├─ exception/
   │  │     ├─ model/         # JPA entities
   │  │     ├─ repository/    # Spring Data JPA repos
   │  │     ├─ security/      # CustomUserDetails + login success handler
   │  │     ├─ service/
   │  │     ├─ serviceImpl/
   │  │     └─ util/
   │  ├─ resources/
   │  │  ├─ application.properties
   │  │  └─ static/
   │  │     ├─ css/style.css
   │  │     └─ js/script.js
   │  └─ webapp/
   │     └─ WEB-INF/views/
   │        ├─ auth/
   │        ├─ user/
   │        ├─ admin/
   │        ├─ common/
   │        └─ error/
   └─ test/
      └─ java/com/project/complaintsystem/
```

## Run locally

### Prerequisites
- JDK 21
- MySQL 8+

### 1) Configure database env vars

This project reads its datasource settings from environment variables (see `src/main/resources/application.properties`).

Required:
- `MYSQLHOST`
- `MYSQLPORT`
- `MYSQLDATABASE`
- `MYSQLUSER`
- `MYSQLPASSWORD`

Optional:
- `PORT` (defaults to `8080`)

PowerShell example:

```powershell
$env:MYSQLHOST = "localhost"
$env:MYSQLPORT = "3306"
$env:MYSQLDATABASE = "complaint_system"
$env:MYSQLUSER = "root"
$env:MYSQLPASSWORD = "your_password"
$env:PORT = "8080"
```

### 2) Make sure schema + seed data exist

Important: JPA is configured with `spring.jpa.hibernate.ddl-auto=validate`, so the application expects the tables to already exist.

At minimum you should ensure:
- A `categories` row exists (otherwise the submit form will have no categories).
- Roles are auto-seeded on startup (`ROLE_CITIZEN`, `ROLE_ADMIN`) once the `roles` table exists.

Example category insert:

```sql
INSERT INTO categories (name, description) VALUES
    ('Road', 'Road-related issues'),
    ('Water', 'Water supply issues'),
    ('Electricity', 'Electricity issues');
```

If you’re starting from scratch for local development, the simplest approach is to temporarily change `spring.jpa.hibernate.ddl-auto` from `validate` to `update` (or `create`) and then switch it back once your schema is in place.

### 3) Start the app

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Then open:
- `http://localhost:8080/login`

## Usage

- Register at `/register` and choose a role:
    - `Citizen` (access: `/user/**`)
    - `Admin` (access: `/admin/**`)
- After login, you’ll be redirected based on role.

## Demo accounts

These accounts are meant for demo/testing and may be reset at any time.

- Citizen: `rohan@gmail.com` / `password123`
- Admin: `admin@smartcomplaint.com` / `admin123`

## Admin report export

The Excel export endpoint is:

`/admin/reports/download?from=yyyy-MM-dd&to=yyyy-MM-dd`

Example:

`/admin/reports/download?from=2026-01-01&to=2026-01-31`

## Build

Windows:

```powershell
.\mvnw.cmd -DskipTests package
```

macOS/Linux:

```bash
./mvnw -DskipTests package
```

## Tests

Windows:

```powershell
.\mvnw.cmd test
```

macOS/Linux:

```bash
./mvnw test
```

Note: tests load the Spring context and will require valid MySQL env vars + an accessible database.

## Deployment

The live app is deployed on Railway:
https://complaint-system.up.railway.app/

Railway typically provides `PORT` and can provide MySQL connection env vars (or you can set them in the Railway service settings).

> “Great things in business are never done by one person; they’re done by a team of people.”

Made with love by the team.