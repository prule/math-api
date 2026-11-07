# Proposal

## Why

`/v3/api-docs` and Swagger UI are always on and unauthenticated. That's right for local development, but an environment exposed beyond developers should be able to turn the docs off without a code change. Exposing the full contract and an interactive request console to the public is an avoidable attack surface. The `add-api-docs` review flagged this as the next step once the API is deployed anywhere.

## What Changes

- API documentation (both `/v3/api-docs` and Swagger UI) stays **on by default**. Running locally (`./gradlew bootRun`, tests) is unchanged.
- A new Spring profile **`prod`** turns both off. Activate it with `SPRING_PROFILES_ACTIVE=prod` or `--spring.profiles.active=prod`.
- When docs are off, `GET /v3/api-docs`, `GET /swagger-ui.html` and `/swagger-ui/**` return **404**. The calculation API and the health check are unaffected.
- The two springdoc switches can also be overridden on their own with standard Spring configuration (e.g. `SPRINGDOC_SWAGGER_UI_ENABLED=false` leaves the JSON document on and hides only the UI). This is Spring Boot's own property binding, so no new code is needed.

**Out of scope:** authentication or IP restrictions on the docs, other `prod`-profile settings (logging, actuator, server tuning), deployment and container config, and docs for other environments (`staging`, etc.), which can reuse the same two properties.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `api-documentation`: "OpenAPI document served" and "Interactive documentation served" apply only when documentation is enabled, which it is by default. A new requirement covers disabling it per environment.

## Impact

- **Code:**
  - New `src/main/resources/application-prod.yaml` with `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false`.
  - New test `ApiDocumentationDisabledTest` (`@SpringBootTest` + `@ActiveProfiles("prod")`).
  - No Java production code changes.
- **API:** no change by default. Under `prod`, the docs endpoints return 404.
- **Dependencies:** none.
- **Docs:**
  - `docs/PRD.md` §3.3: docs are on by default and off under the `prod` profile. Add a change log row.
  - `docs/STANDARDS.md`: a new §8 "Configuration & profiles": the default config is for local development, environment overrides go in `application-<profile>.yaml`, and `prod` is the first profile. Renumber the change log to §9, and add a change log row.
  - `docs/BACKLOG.md`: add this as a done item under v1 follow-ups.
  - `docs/DOMAIN-MODEL.md`: none.
