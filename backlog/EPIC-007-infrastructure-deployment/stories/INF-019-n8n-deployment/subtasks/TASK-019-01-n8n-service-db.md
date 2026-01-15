# TASK-019-01: n8n service + database init

## Goal
Zavest n8n service v docker-compose a pripravit samostatnou databazi s dedikovanym uzivatelem.

## Tasks
- [ ] Pridat n8n service do `docker/docker-compose.yml` (image, volumes, networks, depends_on, healthcheck).
- [ ] Definovat env promennne pro DB (`N8N_DB_USERNAME`, `N8N_DB_PASSWORD_FILE`, `N8N_DB_NAME`) a zapsat do `.env.example`.
- [ ] Vytvorit init skript pro DB (napr. `docker/db/init/15-n8n.sh`) pro vytvoreni DB `n8n` a role z env/secret file.
- [ ] Pridat persistentni volume `n8n-data` pro `/home/node/.n8n`.
- [ ] Overit start n8n service bez chyb v logu.

## Output
- n8n service v compose s persistentnim ulozistem.
- DB `n8n` a dedikovany uzivatel vytvoreny z env/secrets.

## Acceptance Criteria for This Subtask
- [ ] `docker compose up n8n` nastartuje bez erroru.
- [ ] n8n pouziva DB `n8n` a samostatneho DB uzivatele.
- [ ] DB heslo je nacitano z secret file nebo env (ne hardcoded v compose).
- [ ] Data n8n zustavaji po restartu v `n8n-data` volume.
