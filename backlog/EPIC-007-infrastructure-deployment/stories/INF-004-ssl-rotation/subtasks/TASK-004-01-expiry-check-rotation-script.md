# TASK-004-01: Expiry check + rotation script

## Goal
Pridat skript, ktery kontroluje expiraci certifikatu a umi automaticky rotovat.

## Tasks
- [ ] Implementovat `scripts/ssl/check-and-rotate.sh` s vypoctem expirace.
- [ ] Nastavit thresholdy (alert <30d, rotate <7d).
- [ ] Ulozit backup stareho certifikatu pred rotaci.

## Output
- Funkcni script pro kontrolu a rotaci certifikatu.

## Acceptance Criteria for This Subtask
- [ ] Skript korektne vypocita pocet dni do expirace.
- [ ] Pri expiraci <7 dni vygeneruje novy cert.
- [ ] Stary cert je ulozen v backup souboru.
