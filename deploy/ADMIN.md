# Granting moderator access

Moderators use a **dedicated account** for the console, never the account on
their phone. One account holds one session (last login wins), so signing in to
the console with a personal account would sign that phone out.

There is no endpoint that grants the role. It is a deliberate database change.

1. Sign up the moderator account like any user (the app, or `POST /auth/signup`)
   and verify its email.
2. Promote it:

```bash
aws ssm start-session --target <instance-id> --region eu-north-1
cd /opt/devhub
docker compose -f compose.prod.yaml --env-file app.env --env-file image.env \
  exec -T postgres psql -U devhub -d devhub -c \
  "UPDATE users SET role = 'ADMIN' WHERE username = '<username>' AND deleted_at IS NULL"
```

`UPDATE 1` means it worked. The role is carried in the access token, so it takes
effect at the account's next login (or next refresh, at most 15 minutes).

To revoke access, set `role = 'USER'` the same way. An existing console session
keeps admin access until its access token expires.
