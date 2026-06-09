# How to contribute

SapienTerm is a small open-source project and we welcome bug reports, feature requests, and pull requests.

## Getting started

- Make sure you have a [GitHub account](https://github.com/signup/free)
- [Open an issue](https://github.com/LogicalSapien/sapienterm/issues) if one doesn't already exist
- Fork the repository on GitHub and then clone:
  - `git clone git@github.com:your-username/sapienterm.git`
- Build for the first time (requires JDK 17):
  - `./gradlew assembleOssDebug`
- Run the unit tests:
  - `./gradlew test`

## Making changes

- Create a topic branch from `main`:
  - `git checkout -b my-fix main`
- Make commits of logical units with clear messages
- Make sure no new Android lint issues appear:
  - `./gradlew lint`
- Make sure all checks and tests pass:
  - `./gradlew check test`

## Submitting changes

- Push your changes to a topic branch in your fork
- Open a [pull request](https://github.com/LogicalSapien/sapienterm/compare/) against `main`

## Questions / discussion

Open a [GitHub Discussion](https://github.com/LogicalSapien/sapienterm/discussions) for questions or ideas.
