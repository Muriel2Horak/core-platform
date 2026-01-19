# TASK-005-02: Certificate storage + renewal

## Goal
Zajistit bezpecne ulozeni certifikatu a automatickou obnovu.

## Tasks
- [ ] Nastavit volume pro `acme.json` s prava 600.
- [ ] Pridat kontrolni skript pro expiraci a trigger obnovy.
- [ ] Zajistit backup `acme.json` (offsite nebo rotate).

## Output
- Bezpecne ulozene certifikaty a obnovovaci mechanismus.

## Acceptance Criteria for This Subtask
- [ ] `acme.json` ma omezeny pristup (600).
- [ ] Obnova certifikatu probiha bez manualniho zasahu.
- [ ] Existuje postup/skript pro zalohu `acme.json`.
