# Sprint plan

Four two-week sprints for a team of 4 developers, 1 QA and a product owner.
Velocity was forecast at about 25 points per sprint, based on the team's
history.

## Sprint 1: "A user can log in and browse" (26 points)

Goal: a deployed skeleton that proves the architecture end to end.

| Story | Points |
|---|---|
| TS-503 Docker Compose stack | 5 |
| TS-504 CI pipeline | 5 |
| US-101 Register | 3 |
| US-102 Log in | 3 |
| US-103 Protect endpoints (gateway) | 5 |
| US-201 Browse and search | 5 |

## Sprint 2: "A user can place an order" (24 points)

Goal: the happy path of the saga works.

| Story | Points |
|---|---|
| US-301 Cart | 3 |
| US-302 Place an order | 8 |
| US-303 Reserve stock | 8 |
| US-304 Take payment | 5 |

## Sprint 3: "Failures are handled" (26 points)

Goal: no lost events, no stuck orders, no double charges.

| Story | Points |
|---|---|
| US-305 Roll back failed orders | 8 |
| TS-501 Transactional outbox | 8 |
| TS-502 Dead-letter topics | 3 |
| US-306 Follow my order live | 5 |
| US-202 Stock on product page | 2 |

## Sprint 4: "Operate it" (25 points)

Goal: admins can manage the catalog, and the system runs in the cloud.

| Story | Points |
|---|---|
| US-203 Admin adds a product | 5 |
| US-204 Admin restocks | 2 |
| US-401 Notifications | 5 |
| TS-505 Kubernetes and cloud | 13 |

## Scrum events

| Event | When | Length | Purpose |
|---|---|---|---|
| Sprint planning | Day 1 | 2 h | Pick stories that meet the sprint goal; break them into tasks |
| Daily scrum | Every day | 15 min | What I did, what I will do, what is blocking me |
| Backlog refinement | Mid-sprint | 1 h | Clarify and estimate upcoming stories so they meet the Definition of Ready |
| Sprint review | Last day | 1 h | Demo working software to the product owner and stakeholders |
| Retrospective | Last day | 1 h | What went well, what to improve, one concrete action |

## Example retrospective (after sprint 2)

- **Went well:** the saga happy path worked on the first demo; pairing on the Kafka setup spread the knowledge.
- **To improve:** we found the "order stuck in PENDING" bug in the demo, not in testing.
- **Action:** add TS-501 (transactional outbox) to sprint 3, and add the saga failure paths to the smoke test.
