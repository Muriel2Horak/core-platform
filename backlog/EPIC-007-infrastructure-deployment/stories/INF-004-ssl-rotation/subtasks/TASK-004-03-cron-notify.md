# TASK-004-03: Cron schedule + notifications

## Goal
Zavedeni pravidelne kontroly expirace a notifikaci.

## Tasks
- [ ] Pridat cron entry pro denni/tydenni kontrolu.
- [ ] Napojit notifikaci (Slack/email) pri rotaci nebo blizici se expiraci.
- [ ] Zdokumentovat provozni postup.

## Output
- Automaticky schedule pro kontrolu certu.

## Acceptance Criteria for This Subtask
- [ ] Cron job bezi a loguje vysledek kontroly.
- [ ] Notifikace se posila pri expiraci pod threshold.
- [ ] Runbook popisuje kde najit logy a jak provest manual rotaci.
