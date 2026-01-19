# TASK-022-02: GraphQL schema + resolvers

## Goal
Navrhnout schema a napojit resolvery na backend API.

## Tasks
- [ ] Definovat zakladni GraphQL schema (dashboard, user, workflows).
- [ ] Implementovat resolvery s paralelnimi REST calls.
- [ ] Pridat DataLoader pro batching.

## Output
- GraphQL API s agregaci dat.

## Acceptance Criteria for This Subtask
- [ ] Jeden GraphQL request nahradi vice REST volani.
- [ ] DataLoader snizi N+1 problem.
- [ ] Schema ma typy pro hlavni obrazovky.
