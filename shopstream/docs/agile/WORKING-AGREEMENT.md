# Working agreement

## Definition of Ready (before a story enters a sprint)

- It is written as a user story, or as a technical story that explains why it is needed.
- The acceptance criteria are testable (Given / When / Then).
- The team has estimated it, and it fits in one sprint (8 points or less; split it otherwise).
- Dependencies (other teams, API changes, designs) are known.

## Definition of Done (before a story counts as finished)

- The code is merged to `develop` through a reviewed pull request (at least 1 approval).
- The CI pipeline is green: build, unit tests, frontend tests.
- New logic has unit tests; new endpoints have at least one web test or an entry in `api-requests.http`.
- Database changes are a new Flyway migration (never an edit to an old one).
- Event contract changes are backward compatible (only add fields), or every consumer is updated in the same release.
- `./scripts/smoke-test.sh` passes against the local stack.
- Docs (README, ARCHITECTURE, backlog) are updated if behavior changed.
- The product owner has accepted it in the sprint review.

## Branching and releases

See [CONTRIBUTING.md](../../CONTRIBUTING.md).

## Code review checklist

- Does it meet the acceptance criteria?
- Are errors handled, and do they return the right HTTP status?
- Is the Kafka consumer idempotent if the message arrives twice?
- Are there secrets, passwords or tokens in the diff? (There must be none.)
- Would the next developer understand it without asking you?
