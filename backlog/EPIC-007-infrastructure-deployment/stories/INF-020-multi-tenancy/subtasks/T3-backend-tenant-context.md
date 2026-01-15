# T3: Backend Tenant Context + Guard

## Goal
Zavest tenant context v backendu a vynutit izolaci napric requesty a databazi.

## Tasks
- [ ] Implementovat filter/interceptor pro nacteni tenantu z `Host` nebo `X-Tenant-ID`.
- [ ] Overit existenci tenantu a vratit 404 pro neznamy tenant.
- [ ] Ulozit tenant do thread-local contextu a vycistit po requestu.
- [ ] Zajistit tenant filtr v DB dotazech (row-level security nebo query filter).
- [ ] Pridat integra testy pro cross-tenant access a tenant missing.

## Output
- Tenant context v backendu dostupny pro vsechny requesty.
- Guard proti pristupu mezi tenanty.

## Acceptance Criteria for This Subtask
- [ ] Request s neznamym tenantem vraci 404.
- [ ] `TenantContext` je nastaven pro kazdy request a vycisten po response.
- [ ] DB dotazy nefunguji bez tenant kontextu.
- [ ] Testy potvrdi, ze tenant-A nevidi data tenant-B.
