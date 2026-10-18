# Agent Rules & Guidelines

This document outlines the project-specific guidelines, architecture, and coding standards for the Sakshi Portal application.

## Core Rules

1. do not change any rule in AGENT.md without permission
2. **Entire Project must use English only**: The entire project must use English only for all source code, comments, commit messages, documentation, and pull requests.
3. **Clean & Structured Code**: Keep the project clean, structured, modern, and production-ready.
4. **Latest Stable Android APIs**: Always prefer the latest stable Android APIs. If a stable version is unavailable, using a Release Candidate (RC) is acceptable.
5. **Modern Android Architecture & Kotlin Best Practices**: Follow the official Android architecture guidelines.
6. **Documentation**: Every public API and important class should be documented.
7. Write clear KDoc for all public classes, interfaces, functions, properties, and modules. Keep documentation concise, accurate, and update it whenever the API changes.
8. **Contributor-Friendly**: Keep the repository contributor-friendly.
9. **Version Catalogs**: Dependencies should be defined in the Version Catalog (`libs.versions.toml`) and referenced through `libs.*`; avoid declaring dependency coordinates directly in build files unless there is a specific reason.
10. do not downgrade any verison of GitHub workflow

## Clean Architecture

11. **Modularity**: Never place unrelated logic into a single file. Keep the project modular, structured, and maintainable by separating responsibilities into appropriate classes and packages.

**STRICT RULE:**
Do not downgrade any dependency, plugin, library, SDK, Gradle version, Kotlin version, Android version, GitHub Action version, or other project/tooling version without explicit permission from the user. Never downgrade a version merely to solve a compatibility issue, build error, warning, or test failure. Ask the user first if a downgrade is genuinely required.
