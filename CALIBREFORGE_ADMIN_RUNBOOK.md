# CalibreForge Account Administration Runbook

## Private Studio access

Studio and pg-meta run only on the backend Docker network. Caddy does not publish Studio's `/project/*` route. The Kong port is loopback-only on the VPS and its dashboard route requires Kong Basic Auth (an unauthenticated probe returned HTTP 401).

1. On the administrator workstation, start an SSH tunnel:

   ```sh
   ssh -N -L 18000:127.0.0.1:18000 rudy@104.166.88.31
   ```

2. Open `http://127.0.0.1:18000/project/default/` in a browser. If needed, retrieve only the dashboard Basic Auth values locally with `sudo grep -E '^(DASHBOARD_USERNAME|DASHBOARD_PASSWORD)=' /srv/hosting/nuvio-backend-staging/.env`; never paste the output into chat.
3. In Studio's SQL Editor, use the following queries. Replace the UUID placeholder with the account ID shown in Authentication > Users. Keep IDs and emails inside the authenticated admin session.

## List and search

```sql
SELECT id, email, created_at, last_sign_in_at, suspension_recorded, profile_count
FROM calibre_admin.account_summaries(NULL)
WHERE email ILIKE '%search text%'
ORDER BY created_at DESC;
```

This private function is not executable by anon, authenticated, or service_role.

## Suspend an account

Use Studio's Authentication > Users controls to apply a long-duration ban so new sign-ins are blocked. Then record the app-level suspension and revoke all existing refresh sessions:

```sql
BEGIN;
INSERT INTO calibre_admin.account_status (account_id, suspended)
VALUES ('<ACCOUNT_UUID>'::uuid, true)
ON CONFLICT (account_id) DO UPDATE
SET suspended = true, updated_at = now();
SELECT calibre_admin.revoke_sessions('<ACCOUNT_UUID>'::uuid);
COMMIT;
```

The installed REST pre-request guard and Storage access helper check this status and the JWT session row on every applicable request, so existing access JWTs cannot continue syncing after revocation.

## Reactivate an account

Clear the ban in Studio's Authentication > Users controls, then remove the app-level suspension marker. The user must sign in again because the previous sessions were revoked.

```sql
DELETE FROM calibre_admin.account_status
WHERE account_id = '<ACCOUNT_UUID>'::uuid;
```

## Revoke sessions without suspension

```sql
SELECT calibre_admin.revoke_sessions('<ACCOUNT_UUID>'::uuid);
```

Use Studio only through the SSH tunnel. Do not add the Studio route to public Caddy routing or grant the private admin functions to application roles.
