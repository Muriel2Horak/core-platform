#!/bin/bash
# RYCHLÉ ŘEŠENÍ - Profile Image Fix

echo "🚀 Quick Fix - Profile Image Mapper"
echo "=================================="

# Přihlášení do Keycloak kontejneru a přidání mapperu
docker exec core-keycloak /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin

# Přidání mapperu
docker exec core-keycloak /opt/keycloak/bin/kcadm.sh create client-scopes/$(docker exec core-keycloak /opt/keycloak/bin/kcadm.sh get client-scopes -r core-platform --fields id,name | grep '"name" : "profile"' -B2 | grep '"id"' | cut -d'"' -f4)/protocol-mappers/models -r core-platform \
  -s name=customProfileImage-mapper \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-usermodel-attribute-mapper \
  -s 'config."user.attribute"=customProfileImage' \
  -s 'config."claim.name"=customProfileImage' \
  -s 'config."id.token.claim"=true' \
  -s 'config."access.token.claim"=true' \
  -s 'config."userinfo.token.claim"=true'

echo "✅ Done! Now logout and login again to test."