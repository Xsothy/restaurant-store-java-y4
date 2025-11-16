# Repository Guidelines

This repository contains a Spring Boot REST API with Tailwind-based HTML templates for a restaurant store application.

## Project Structure & Module Organization
- Backend Java code: `src/main/java` (controllers, services, DTOs, repositories).
- Backend tests: `src/test/java`, mirroring the `src/main/java` package structure.
- Templates and static assets: `src/main/resources` (including generated CSS under `static/css`).
- Tailwind source: `src/main/tailwind` built via Node tooling defined in `package.json`.
- Additional architecture and process notes live in the root `README*.md`, `IMPLEMENTATION*.md`, and `MERGE_*` documents.

## Build, Test, and Development Commands
- `mvnw.cmd clean verify` (Windows) / `./mvnw clean verify` (Unix): compile, run tests, and build the application.
- `mvnw.cmd spring-boot:run` / `./mvnw spring-boot:run`: run the Spring Boot API locally.
- `npm install`: install Node/Tailwind dependencies (run once or when dependencies change).
- `npm run build`: build Tailwind CSS from `src/main/tailwind` into `src/main/resources/static/css/tailwind.css`.

## Coding Style & Naming Conventions
- Use Java 17, 4-space indentation, and braces on the same line as declarations.
- Follow existing package layout: controllers in `.controller`, DTOs in `.dto`, services in `.service`, repositories in `.repository`.
- Classes: `PascalCase`; methods and fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Prefer explicit imports (no wildcards) and idiomatic Spring annotations (`@RestController`, `@Service`, `@Repository`, etc.).

## Testing Guidelines
- Use JUnit and Spring Boot test support; place tests under `src/test/java` with names ending in `*Test`.
- Keep tests focused and deterministic; mock external dependencies where appropriate.
- Run `mvnw.cmd test` / `./mvnw test` or the full `clean verify` before opening a pull request.

## Commit & Pull Request Guidelines
- Write concise, imperative commit messages (e.g., `Fix order tracking template initialization`).
- Group related changes into small, focused commits; avoid mixing refactors with feature work.
- Pull requests should include: a clear summary, linked issues or task IDs, test notes, and screenshots for UI/template changes.

## Agent-Specific Instructions
- Before large changes, review `IMPLEMENTATION.md`, `INTEGRATION_README.md`, `SWAGGER_SETUP.md`, and related docs for context.
- Prefer minimal, targeted diffs that preserve existing architecture and naming conventions.
- Do not introduce new frameworks or major dependencies without explicit justification in the PR description.

