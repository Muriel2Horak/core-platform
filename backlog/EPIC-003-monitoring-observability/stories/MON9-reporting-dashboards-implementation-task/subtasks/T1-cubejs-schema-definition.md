# T1: Cube.js Schema Definition
**Effort:** ~6h | **LOC:** ~600

## Goal
Definovat Cube.js data modely pro analytics.

## Files
- `cube/schema/Revenue.js`
- `cube/schema/Usage.js`
- `cube/schema/Compliance.js`
- `cube/schema/Tenants.js`
- `cube/schema/Users.js`

## Tasks
- [ ] Navrhnout dimenze a measures pro revenue/usage/compliance.
- [ ] Pridat pre-aggregations a materialized view strategy.
- [ ] Nastavit tenant scoping v Cube.js query layer.
- [ ] Overit performance v Cube.js playground.

## Output
- Validni Cube.js schema s tenant izolaci a pre-aggregations.

## Acceptance Criteria
- Cube.js playground funguje
- Všechny measures validní
- Query performance < 1s
- Pre-aggregations configured
