# TASK-016-01: Backup schedule + storage

## Goal
Zavest pravidelne zalohy DB a upload do S3/MinIO.

## Tasks
- [ ] Implementovat `scripts/backup/pg-backup.sh` pro full backup.
- [ ] Nastavit schedule (cron) pro daily/6h backup.
- [ ] Konfigurovat S3/MinIO bucket a retention.

## Output
- Automatizovane zalohy s offsite ulozenim.

## Acceptance Criteria for This Subtask
- [ ] Zaloha se spusti podle harmonogramu.
- [ ] Backupy jsou ulozene v S3/MinIO.
- [ ] Retence odstrani stare zalohy.
