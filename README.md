# math-api

> **This is a demonstration project, not a real product.** It was built as part of a presentation showing how [OpenSpec](https://openspec.dev), grill-me and ponytail review can take an idea to a working, tested API with Claude Code. The code, docs and specs are real and the build passes, but the service isn't deployed, maintained or meant for production use. The presentation is in [docs/slides/](docs/slides/).

A stateless REST API for exact decimal arithmetic on one or two operands, built with Spring Boot 4 and Java 25.

- Numbers are `BigDecimal` throughout: `0.1 + 0.2` is `0.3`, not `0.30000000000000004`.
- Results come back as JSON strings, so clients never lose precision.
- Every error is [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) with a stable `type`.
- Operand and request sizes are bounded, so no request can run away with CPU or memory.

For the supported operations and their rules, see [PRD §3.1](docs/PRD.md#31-operations) or the running API's Swagger UI.

## Run it

Requires Java 25. The Gradle wrapper does the rest.

```bash
./gradlew build      # tests, ArchUnit layer rules, Spotless formatting
./gradlew bootRun    # serves on http://localhost:8080
```

```bash
curl -X POST localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"operation": "ADD", "a": 0.1, "b": "0.2"}'
```

```json
{"operation":"ADD","a":"0.1","b":"0.2","result":"0.3"}
```

Operands may be JSON numbers or strings. The `operation` name is case-sensitive.

## API docs

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI 3.1 JSON: http://localhost:8080/v3/api-docs

Both are on by default and off under the `prod` profile (`SPRING_PROFILES_ACTIVE=prod`). Deployments must set that profile.

## Documentation

| Doc | What it holds |
|-----|---------------|
| [docs/PRD.md](docs/PRD.md) | Scope, API contract, error types, non-goals, roadmap |
| [docs/DOMAIN-MODEL.md](docs/DOMAIN-MODEL.md) | Glossary and business rules (BR-n) |
| [docs/STANDARDS.md](docs/STANDARDS.md) | Architecture, conventions, testing, configuration |
| [docs/BACKLOG.md](docs/BACKLOG.md) | Ordered changes, each with a ready `/opsx:propose` prompt |
| [openspec/specs/](openspec/specs/) | Current behaviour specs, one folder per capability |

## How changes are made

Every change follows the workflow in [CLAUDE.md](CLAUDE.md). It's planned with [OpenSpec](https://openspec.dev), then:
1. the plan and the code are each reviewed with ponytail
2. the change is archived into the specs with a retrospective
3. it's merged only when CI is green

Past changes, with their designs and retrospectives, are in [openspec/changes/archive/](openspec/changes/archive/).
