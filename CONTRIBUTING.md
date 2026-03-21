# Contributing to PiAware Flight Tracker

Thanks for your interest in contributing! This document covers the process for submitting changes.

## Getting Started

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Submit a pull request

## Development Setup

- JDK 17+
- Android SDK (API 24+)
- Run `./gradlew ktlintCheck detekt` before committing to catch style issues
- Run `./gradlew ktlintFormat` to auto-fix most formatting problems

## Code Style

- Kotlin code follows [ktlint](https://pinterest.github.io/ktlint/) conventions
- No wildcard imports
- [Detekt](https://detekt.dev/) is configured for static analysis
- Compose functions follow standard naming conventions (PascalCase)

## Pull Request Process

1. All PRs target the `main` branch
2. Branch protection requires at least one review before merging
3. CI must pass: lint, unit tests, desktop UI tests, and Android instrumented tests
4. Keep PRs focused - one logical change per PR
5. Write clear commit messages explaining **why**, not just what

## Testing Requirements

PRs that add or modify functionality should include tests:

- **Unit tests** (`commonTest/`) for business logic, ViewModels, use cases, and repositories
- **Desktop UI tests** (`desktopTest/`) for new composable screens and components
- **Android instrumented tests** (`androidTest/`) for smoke tests on real Android rendering

Run the full test suite before submitting:

```bash
./gradlew ktlintCheck detekt :composeApp:testDebugUnitTest :composeApp:desktopTest
```

## Reporting Issues

Use [GitHub Issues](../../issues) to report bugs or request features. Include:

- Steps to reproduce (for bugs)
- Expected vs actual behavior
- Platform and device information
- PiAware receiver version if relevant

## License

By contributing, you agree that your contributions will be licensed under the [GPL v2.0](LICENSE).
