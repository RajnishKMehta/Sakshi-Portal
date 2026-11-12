# Contributing to Sakshi Portal

Thank you for your interest in contributing to **Sakshi Portal**! We welcome and appreciate contributions of all forms, including bug reports, feature requests, documentation improvements, and code changes.

## Before You Start

**Before making any changes or starting development, you must carefully read the complete [Sākṣī Contribution Guidelines](https://sakshi-docs.gitbook.io/space/developer-guide/contributing/contributing).** Read the entire document, understand all its instructions, requirements, and contribution policies, and follow them throughout your contribution.

Do not skip any sections or start implementing changes without understanding the documented guidelines. All contributions must comply with the central contribution guidelines as well as the repository-specific instructions provided below.

## Code Guidelines

- **English Only**: All source code, code comments, documentation, and commit messages must be in English.
- **Best Practices**: Keep code clean, modern, and production-ready.
- **Kotlin Standard**: Follow modern Kotlin style guides and use structured coroutines for background tasks.
- **KDoc**: Add or update proper KDoc documentation for any new or modified public APIs, important classes, functions, and other relevant code.
- **Testing**: Maintain high test coverage. Ensure all unit and Robolectric tests pass before submitting code.

## Localization and Translations

**Sakshi Vault is not accepting translations or localization contributions.** The application is intended to remain in its existing language, and contributions must not introduce support for additional languages.

- Do not translate any application strings into other languages.
- Do not create language-specific resource directories or files such as `02_vault/app/src/main/res/values-*/strings.xml`.
- Do not add, modify, or introduce localized string resources for additional languages.
- Keep application strings in the existing default resource file: `02_vault/app/src/main/res/values/strings.xml`.
- Do not introduce translation frameworks, localization configurations, or other translation-related changes.

**Any Pull Request that introduces translations, additional language resources, or localization support will not be accepted or merged.** Please do not submit translation-related contributions.

## Submitting Pull Requests

1. Fork the repository and create your branch from `main`.
2. Commit your changes with a clear and descriptive commit message.
3. Verify your changes using `./gradlew test` and `./gradlew assembleDebug`.
4. Ensure your implementation follows the central [Sākṣī Contribution Guidelines](https://sakshi-docs.gitbook.io/space/developer-guide/contributing/contributing) and all repository-specific requirements.
5. Open a Pull Request referencing the issue you are addressing.

   Make sure your Pull Request clearly describes the changes, the reason behind them, and any relevant testing performed.

   If your code changes require corresponding updates to the central documentation, submit a separate documentation Pull Request to the [Sakshi-Docs repository](https://github.com/RajnishKMehta/Sakshi-Docs) alongside your main Pull Request, as required by the central contribution guidelines.
