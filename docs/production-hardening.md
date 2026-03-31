# Backend production hardening

## Required environment

- Bind the backend privately on `127.0.0.1:8080`.
- Put Nginx in front of the app and expose only `80/443`.
- Set `APP_TODO_BASE_URL=https://your-domain.com`.
- Set `APP_TODO_DEEP_LINK_PATTERN=https://your-domain.com/todos/%d`.
- Set `APP_CORS_ALLOWED_ORIGINS=https://your-domain.com`.
- Set `APP_WEBSOCKET_ALLOWED_ORIGINS=https://your-domain.com`.
- Set `SERVER_FORWARD_HEADERS_STRATEGY=framework`.
- Set the production PostgreSQL and SMTP variables from secrets, not committed files.

## Local Postgres

- The default non-prod app config now targets `jdbc:postgresql://127.0.0.1:5432/todo`.
- Start only the local database with `docker compose up -d todo-db`.
- Use the localhost values in [`.env.example`](/Users/rvikas/IdeaProjects/Todo/.env.example) for local runs.
- Tests still use isolated H2 settings from [`src/test/resources/application.properties`](/Users/rvikas/IdeaProjects/Todo/src/test/resources/application.properties).

## What this repo now supports

- Flyway runs automatically in `prod`.
- Actuator health is exposed at `/actuator/health`.
- Liveness and readiness probes are enabled.
- API rate limiting is configurable with `APP_RATE_LIMIT_*`.
- Rolling file logging is configurable with `LOGGING_FILE_NAME` and `LOGGING_ROLLING_*`.
- Docker Compose binds Postgres and the backend only on loopback.
- `deploy/nginx/todo.conf` includes proxy headers and WebSocket upgrade handling.
- `deploy/systemd/todo-backend.service` restarts the backend on failure or reboot.

## Manual go-live checks

1. Open `https://your-domain.com/todos`.
2. Open `https://your-domain.com/todos/{id}`.
3. Create, update, and list todos from the deployed frontend.
4. Verify `/ws` live updates through Nginx.
5. Trigger a real reminder email and confirm both links work:
   - `Open in Todo` should land on the frontend deep link.
   - `Complete from email` should complete the todo through the backend endpoint.
6. Verify `/actuator/health` returns `UP`.
7. Confirm SMTP sender domain has SPF and DKIM configured.
8. Add uptime and mail-failure alerts in your hosting provider or monitoring stack.
