# BorrowBox

BorrowBox is a campus rental marketplace where students can list, discover, and request items to borrow. Built with React, Spring Boot, and MySQL, with authentication, image uploads, rental requests, and an optional Gemini-powered assistant. Payments are simulated for demonstration.

BorrowBox is a college-focused peer-to-peer rental marketplace. Students can share useful equipment, request rentals, record a simulated payment, mark a return, and leave a review. It uses a React frontend, a Spring Boot REST API, and MySQL configured for Aiven Cloud MySQL.

## Features

- Student registration, login, JWT authentication, password hashing, and profile editing
- Searchable item listings with category, price, availability, condition, and rating filters
- Create, edit, pause, and remove owned listings; upload JPG, PNG, WEBP, or GIF listing photos (up to 5 MB); admin moderation can pause listings
- Rental requests with server-calculated rental fees and security deposits, overlap checks, owner decisions, simulated payment, handover, return, and review
- Payment history, borrower and owner dashboards, and admin statistics/tables
- Search and item-view history with available-only, activity-aware recommendations
- BorrowBox assistant that returns only actual database listings; with an AI key it uses an OpenAI-compatible chat-completions endpoint to extract search intent
- AI listing-description drafts when an API key is configured; a clearly labeled basic editable draft is available without one
- Docker images for the frontend and backend; the database remains Aiven MySQL

## Stack and architecture

- React, JavaScript, React Router, Axios, Vite
- Java 17+, Spring Boot, Spring Web, Spring Security, Spring Data JPA, Hibernate
- MySQL / Aiven Cloud MySQL over TLS
- OpenAI-compatible AI API called only from Spring Boot

```text
Browser → React/Vite → REST API (Spring Boot) → Aiven MySQL
                                  └────────────→ AI API (optional)
```

## Local setup

### 1. Configure the backend

From the repository folder, copy `backend/.env.example` to `backend/.env`, then edit **`backend/.env`** with your Aiven host, port, database, username, password, and a random `JWT_SECRET` of at least 32 UTF-8 bytes. In Windows Command Prompt, the copy command is:

```cmd
copy backend\.env.example backend\.env
notepad backend\.env
```

Save the values in `backend/.env`; leave `.env.example` as placeholders. The real file is ignored by Git and excluded from the backend Docker build context. Never put credentials in React or this README.

Optional values:

- `ADMIN_EMAIL` and `ADMIN_PASSWORD` bootstrap an admin at startup. Use a unique password of 12–72 UTF-8 bytes; leave both blank if you do not need an admin account.
- `AI_API_KEY` enables AI-based intent extraction and description drafts. The API key stays on the backend. If it is blank, BorrowBox chat still supports common item searches through a simple parser and answers built-in FAQ questions; the description button creates a clearly labeled basic draft instead of claiming it used AI.
- `AI_MODEL` and `AI_API_URL` configure the model and OpenAI-compatible chat-completions endpoint.
- `FRONTEND_ORIGIN` defaults to `http://localhost:5173` for Vite.
- `PORT` defaults to `8900`.
- `UPLOAD_DIR` defaults to `uploads` under the backend working directory. Uploaded files are validated, limited to 5 MB, and served through `/api/uploads/...`.

The listing form can upload an image directly from the computer. A remote image URL is optional; it must point to the actual image file, not a search results or sharing page.

The backend imports `backend/.env` when started with `backend` as the current directory. Aiven credentials and JWT secrets are never hard-coded in Java source.

### 2. Run the backend

Prerequisites: JDK 17 or newer and Maven 3.9+, or Maven Daemon (`mvnd`). If `mvnd` is already on your PATH, open Command Prompt in the repository folder and run:

```cmd
cd backend
mvnd spring-boot:run
```

Use `mvn spring-boot:run` if you have Maven instead. The API listens at `http://localhost:8900`; check `http://localhost:8900/api/health`. Leave this terminal open while using BorrowBox.

If you extracted Maven Daemon to the Downloads folder used during setup and `mvnd` is not recognized, run these commands in that same Command Prompt first (the PATH change lasts only for that window):

```cmd
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "MVND_HOME=C:\Users\Dell\Downloads\maven-mvnd-1.0.6-windows-amd64\maven-mvnd-1.0.6-windows-amd64"
set "PATH=%MVND_HOME%\bin;%JAVA_HOME%\bin;%PATH%"
cd backend
mvnd spring-boot:run
```

If Maven Daemon is in a different folder, change only the `MVND_HOME` value to the folder that contains its `bin` directory.

### 3. Run the frontend

Open a second terminal:

```cmd
cd frontend
npm install
npm run dev
```

Open the Vite URL shown in the terminal, usually `http://localhost:5173`. The frontend defaults to the API at `http://localhost:8900/api`; set `VITE_API_BASE_URL` before building if the API is elsewhere.

## API overview

| Area | Routes |
|---|---|
| Health and auth | `GET /api/health`, `POST /api/auth/register`, `POST /api/auth/login`, `GET/PUT /api/auth/me` |
| Browse | `GET /api/categories`, `GET /api/items`, `GET /api/items/{id}`, `GET /api/reviews/item/{itemId}`, `GET /api/uploads/{filename}` |
| Own listings | `GET /api/items/mine`, `POST /api/items`, `PUT /api/items/{id}`, `DELETE /api/items/{id}`, `POST /api/uploads` (authenticated photo upload) |
| Rentals | `POST /api/rentals`, `GET /api/rentals/my`, `GET /api/rentals/owner`, `PUT /api/rentals/{id}/accept`, `/reject`, `/activate`, `/complete`, `/cancel`, `POST /api/rentals/{id}/pay` |
| Payments and reviews | `GET /api/payments/my`, `POST /api/reviews`, `GET /api/reviews/rental/{rentalId}/mine` |
| Recommendations and AI | `GET /api/recommendations/item/{itemId}`, `GET /api/recommendations/user/{userId}`, `POST /api/chat`, `POST /api/ai/generate-description` |
| Admin | `GET /api/admin/summary`, `/users`, `/items`, `/rentals`, `/payments`; moderation routes under `/api/admin/users` and `/api/admin/items` |

Protected requests use `Authorization: Bearer <accessToken>`. Public registration always creates a student account; admin access is controlled by optional bootstrap values. Item recommendations return available listings only, and personalized recommendations are limited to the signed-in user.

### Rental/payment behavior

The backend calculates rental days and amounts and ignores client-supplied totals. The demo payment records a method and status only; it does not process money or store card details. A borrower requests dates, the owner accepts or rejects, the borrower records a simulated payment, the owner marks handover, and either participant can mark an active rental completed. A completed rental can be reviewed by its borrower.

## Tests

Backend integration tests use an in-memory H2 database and do not connect to Aiven:

```cmd
cd backend
mvnd test
```

The integration flow covers health, registration/login, basic description fallback, photo upload and retrieval, listing/search/delete, server-side rental totals, payment, completion, review, personalized recommendations, and student denial from admin routes.

Build the frontend with:

```cmd
cd frontend
npm run build
```

## Docker

Docker Compose builds the React/Nginx frontend and the Spring Boot backend. It does **not** start a local MySQL container; the backend connects to Aiven. Uploaded photos persist in a named Docker volume.

1. Create and fill `backend/.env` as described above.
2. From the repository root run:

```cmd
docker compose up --build
```

Open `http://localhost:3000`; the API health check is `http://localhost:8900/api/health`. Stop containers with `Ctrl+C`, or run `docker compose down` in another repository-root terminal. The compose file passes the frontend origin and API port to the backend.

## Git, credentials, and screenshots

`.gitignore` excludes `.env`, `.env.*` (while allowing `.env.example`), `node_modules`, `dist`, Maven `target`, and IDE files. Before pushing, verify `backend/.env` is ignored and `backend/.env.example` contains placeholders only. Never commit database passwords, JWT secrets, admin passwords, or AI API keys.

Add demo screenshots under `docs/screenshots/` after running the app locally, then link them here. Suggested captures: home/search results, item details and rental dates, owner request dashboard, and admin overview. Screenshots are intentionally not fabricated in this repository.

## Project layout

```text
backend/src/main/java/com/borrowbox/   Spring controllers, services, entities, repositories, DTOs, security
backend/src/main/resources/            Environment-driven datasource and application config
backend/src/test/                      H2-backed API integration test
frontend/src/                          React routes, reusable marketplace components, Axios API client
backend/Dockerfile                     Spring Boot container build
frontend/Dockerfile                    Vite build and Nginx static hosting
docker-compose.yml                     Frontend/backend services using Aiven MySQL
```

## Future enhancements

Real payment provider integration, image/object storage, email notifications, category administration, deployment-specific domains/secrets, rate limiting, and user-to-user messaging can be added after the college demo scope.
