# Template Syntax Guide

This project standardizes template placeholders for `envsubst` rendering.

## Supported Syntax

- Required variable:
  - `${VAR}`
- Optional variable with default:
  - `${VAR:-default}`

## Unsupported Syntax

- `${VAR:default}` (use `${VAR:-default}` instead)
- `$VAR` in templates (reserve for runtime config files, not templates)

## Template Inventory

The following files are treated as envsubst templates:

- `docker/nginx/nginx-ssl.conf.template`
- `docker/keycloak/realm-admin.template.json`
- `docker/keycloak/realm-core-platform.template.json`

## Validation

Run validation locally:

```bash
make validate-templates
```

It checks:
- Missing variables in `.env`
- Invalid placeholder syntax (`${VAR:default}`)
- Reports `.env` variables unused in templates (warning)

## Migration Guide

1. Replace any `${VAR:default}` with `${VAR:-default}`.
2. Ensure every template variable exists in `.env`.
3. Keep generated files using native syntax:
   - Docker Compose: `${VAR}`
   - Spring: `${VAR:default}`
4. Re-run template validation.

```bash
make validate-templates
```
