## What does this change?

<!-- One or two sentences. Link the story: US-123 -->

## How was it tested?

- [ ] Unit tests added or updated
- [ ] `./mvnw verify` passes
- [ ] `npm run test:ci` passes (if the frontend changed)
- [ ] `./scripts/smoke-test.sh` passes against the local stack

## Checklist

- [ ] Database changes are a new Flyway migration
- [ ] Kafka event changes are backward compatible
- [ ] No secrets in the diff
- [ ] Docs updated if behavior changed
