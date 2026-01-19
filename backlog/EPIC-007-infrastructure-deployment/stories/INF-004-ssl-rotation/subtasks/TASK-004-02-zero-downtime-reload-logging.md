# TASK-004-02: Zero-downtime reload + audit log

## Goal
Zajistit hot reload proxy a audit log o rotaci.

## Tasks
- [ ] Reload Nginx/Traefik bez downtime po vymene certu.
- [ ] Zapsat audit log (timestamp, stary/new expiry).
- [ ] Pridat log output do standardniho log souboru.

## Output
- Rotace bez preruseni provozu s audit zaznamem.

## Acceptance Criteria for This Subtask
- [ ] Proxy reload probehne bez restart loopu.
- [ ] Audit log obsahuje datum a expiraci stareho i noveho certu.
- [ ] Sluzby zustavaji dostupne behem rotace.
