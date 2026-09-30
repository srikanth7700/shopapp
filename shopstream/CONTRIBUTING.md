# Contributing

## Branches

A simplified Git Flow:

| Branch | Purpose |
|---|---|
| `main` | What is in production. Every commit here is built and can be deployed. Protected: changes only through pull requests from `develop` or `hotfix/*` |
| `develop` | Integration branch for the next release. CI runs on every push |
| `feature/US-302-place-order` | One branch per story, from `develop`, merged back through a pull request |
| `hotfix/fix-payment-rounding` | Urgent production fix, from `main`, merged into both `main` and `develop` |

Typical flow:

```bash
git checkout develop
git pull
git checkout -b feature/US-401-notifications
# ... work, commit ...
git push -u origin feature/US-401-notifications
# open a pull request into develop, using the template
```

Keep your branch up to date with `git pull --rebase origin develop` so the pull request stays small and conflict-free.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/):

```
feat(order): cancel orders stuck in INVENTORY_RESERVED after 5 minutes
fix(inventory): lock rows in product-id order to avoid deadlocks
test(payment): cover duplicate InventoryReservedEvent
docs: explain the outbox pattern
chore(ci): cache npm dependencies
```

Types: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`, `ci`. Put the service in the scope when the change touches only one.

## Pull requests

- One story per pull request, ideally under 400 changed lines.
- Fill in the template: what changed, how you tested it, the story id.
- CI must be green and at least one reviewer must approve before merging.
- Prefer "Squash and merge" for feature branches so `develop` has one commit per story.

## Before you push

```bash
./mvnw verify                        # backend tests
(cd frontend && npm run test:ci)     # frontend tests
docker compose up --build -d && ./scripts/smoke-test.sh
```
