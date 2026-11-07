# Design

## Context

- `application.yaml` holds all config today, and there are no profiles. springdoc 3.2.0 is configured there (`springdoc.api-docs.*`, `springdoc.swagger-ui.*`).
- springdoc exposes two independent switches, `springdoc.api-docs.enabled` and `springdoc.swagger-ui.enabled`, both `true` by default. Turning one off removes that part's endpoints and handlers.
- `ApiDocumentationTest` (8 tests) runs with no profile and must keep passing unchanged. It's the "enabled by default" evidence.

## Goals / Non-Goals

**Goals:**
- One obvious way to turn the docs off for a deployed environment, with no code change at deploy time.
- Local development and the existing tests keep working with no extra setup.

**Non-Goals:**
- No custom property (e.g. `math-api.docs.enabled`) or `@ConditionalOnProperty` code. springdoc's own switches already do the job, and Spring Boot's property binding already makes them overridable from environment variables.
- No secured docs (auth or IP allow-list). That belongs with the auth roadmap item.

## Decisions

### D1. Default on, `prod` profile off

New file `src/main/resources/application-prod.yaml`:
```yaml
# Production: don't expose the API contract or an interactive console publicly (api-documentation spec).
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

*Alternative A:* default off, with a `dev` profile to turn it on. Rejected, because every `bootRun`, IDE run and test would need a profile, and the request was to keep it on locally. The risk with default-on is forgetting the profile in production. That's mitigated by naming `SPRING_PROFILES_ACTIVE=prod` in PRD §3.3 and STANDARDS §8, so whoever adds deployment config sees it.

*Alternative B:* a single custom `math-api.docs.enabled` property mapped to both switches. Rejected, because it would be a second name for something springdoc already names, with nothing gained.

### D2. Both switches go off together under `prod`

The JSON document is the actual contract, and Swagger UI only renders it. Turning off only the UI would still expose the contract. Each switch can still be overridden on its own (`SPRINGDOC_API_DOCS_ENABLED`, `SPRINGDOC_SWAGGER_UI_ENABLED`), so an environment can e.g. keep the JSON for internal tooling. That flexibility comes from Spring Boot and needs no code, so it's documented but not specially tested.

### D3. Test the disabled mode with a full context under `@ActiveProfiles("prod")`

`ApiDocumentationDisabledTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("prod")`) asserts:
- 404 for `/v3/api-docs`, `/swagger-ui.html` and `/swagger-ui/index.html`
- that one calculation still returns `"2.25"`
- that health is `UP`

It loads the real `application-prod.yaml`, so it tests the shipped config, not a test-only property. The existing `ApiDocumentationTest` stays as the default-enabled evidence and is unchanged.

The 404s come from Spring MVC's "no handler" path. The existing `ProblemDetailsAdvice` (via `ResponseEntityExceptionHandler`) may render them as Problem Details. The spec only requires the status, so the test asserts 404 and nothing about the body.

## Risks / Trade-offs

- [Someone deploys without `SPRING_PROFILES_ACTIVE=prod`, so the docs are public] → It's documented in PRD §3.3 and STANDARDS §8, and nothing breaks: the docs are read-only and the API is already public in v1. Revisit with deployment config (it could default the profile) or the auth item.
- [`swagger-ui.enabled: false` leaves the swagger-ui webjar reachable at another path, e.g. `/webjars/swagger-ui/**`] → The test only covers springdoc's paths. During implementation, also check `GET /webjars/swagger-ui/index.html` under `prod`. If it's served, record it here and decide whether it matters: it's static UI files with no API contract, since `/v3/api-docs` is off.
- *Checked (task 1.3):* under `prod`, Spring Boot's default `/webjars/**` resource handler still serves the swagger-ui webjar: `/webjars/swagger-ui/index.html` and `/webjars/swagger-ui/swagger-ui-bundle.js` return 200 (with or without the `5.34.0` version segment). That's the *stock* Swagger UI page, whose built-in default URL is the public Petstore demo, not our API. `/v3/api-docs` and `/v3/api-docs/swagger-config` return 404 (Problem Details), so no Math API contract is exposed. **Decision (user, during apply):** block it. `application-prod.yaml` also sets `spring.web.resources.add-mappings: false`, which turns off Spring Boot's default static-resource handlers (`/webjars/**`, `classpath:/static/**`) under `prod`. The app serves no other static content, so nothing else is affected, and the spec's 404 list and the test now include `/webjars/swagger-ui/index.html`. If static content is ever added, revisit this line.
- [Adding a `prod` profile invites unrelated prod settings into this change] → Out of scope. This change adds only the two springdoc lines and the static-resource switch that completes "docs off".

## Open Questions

None.
