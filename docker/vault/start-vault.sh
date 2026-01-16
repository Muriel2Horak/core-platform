#!/usr/bin/env sh
set -e

exec vault server -config=/vault/config/config.hcl
