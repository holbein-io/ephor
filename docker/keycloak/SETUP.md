# Keycloak Manual Setup

After starting the stack with `docker compose -f docker-compose.yml -f docker-compose.oauth2.yml up --build`,
the Keycloak realm is auto-imported from `realm-export.json`. However, service account roles
must be assigned manually (one-time setup).

## Assign service account roles for user directory sync

1. Open Keycloak Admin Console: http://localhost:8180/admin (admin / admin)
2. Select the **ephor** realm (top-left dropdown)
3. Navigate to **Clients** > **ephor-admin**
4. Go to the **Service account roles** tab
5. Click **Assign role**, filter by client **realm-management**
6. Assign the following roles:
   - `view-users`
   - `manage-users`
   - `query-users`
   - `query-groups`
   - `query-realms`

## Add protocol mapper for admin token

1. Navigate to **Clients** > **ephor-admin** > **Client scopes** tab
2. Click **ephor-admin-dedicated**
3. Click **Configure a new mapper** > **User Client Role**
4. Configure:
   - Name: `client-roles`
   - Client ID: `realm-management`
   - Token Claim Name: `resource_access.realm-management.roles`
   - Add to ID token: **ON**
   - Add to access token: **ON**

## Restart the API

After completing both steps, restart the API to pick up the new permissions:

```
docker compose -f docker-compose.yml -f docker-compose.oauth2.yml restart api
```
