# Restoring the database

Two independent layers exist. Prefer the logical dump; fall back to the volume
snapshot only if S3 is unavailable or the dump is corrupt.

`BACKUP_BUCKET` below is the stack's `backup_bucket` output:

```bash
BACKUP_BUCKET=$(terraform -chdir=terraform/envs/prod output -raw backup_bucket)
```

## Layer 1 — nightly logical dump (preferred)

`backup.sh` runs at 02:30 UTC, writes `pg_dump --format=custom` to
`/data/backups`, uploads to `s3://$BACKUP_BUCKET/postgres/`
and prunes local copies older than 7 days. S3 keeps objects for 30 days.

Ordering is deliberate: the dump at 02:30 and the EBS snapshot at 02:45 both
run **before** the app's own 03:30 cleanup jobs, which anonymise deleted
accounts and delete rows. A backup taken after those jobs cannot undo them.

### Drill it (verified 2026-09-21)

```bash
DUMP=$(aws s3 ls s3://$BACKUP_BUCKET/postgres/ --region eu-north-1 | awk '{print $4}' | tail -1)
aws s3 cp "s3://$BACKUP_BUCKET/postgres/$DUMP" /tmp/ --region eu-north-1

docker run -d --name restoredrill -e POSTGRES_PASSWORD=x -e POSTGRES_USER=devhub -e POSTGRES_DB=devhub postgres:18-alpine
docker cp "/tmp/$DUMP" restoredrill:/tmp/d.dump
docker exec restoredrill pg_restore -U devhub -d devhub --clean --if-exists /tmp/d.dump

docker exec restoredrill psql -U devhub -d devhub -tAc \
  "select count(*) from information_schema.tables where table_schema='public'"   # expect 22
docker rm -f restoredrill
```

### Restore into production

```bash
aws ssm start-session --target <instance-id> --region eu-north-1
cd /opt/devhub
docker compose -f compose.prod.yaml --env-file app.env --env-file image.env stop app
aws s3 cp s3://$BACKUP_BUCKET/postgres/<dump> /data/backups/
docker compose -f compose.prod.yaml --env-file app.env --env-file image.env \
  exec -T postgres pg_restore -U devhub -d devhub --clean --if-exists < /data/backups/<dump>
docker compose -f compose.prod.yaml --env-file app.env --env-file image.env start app
```

Stop the app first. Restoring under a live app means Flyway and Hibernate
validation race the restore.

## Layer 2 — EBS snapshot

Daily at 02:45 UTC, 7 retained, taken by Data Lifecycle Manager against the
`Backup=daily` tag. These are crash-consistent, not logically consistent: WAL
replay makes them usable but they cannot be inspected or partially restored.

```bash
aws ec2 create-volume --region eu-north-1 --availability-zone eu-north-1a \
  --snapshot-id <snap-id> --volume-type gp3 --encrypted
# stop the stack, detach the current volume, attach the new one at /dev/sdf, mount, start
```

## What is NOT backed up

- The RSA signing keypair and all other secrets live only in SSM Parameter
  Store. They are regionally durable and survive instance and volume loss, but
  a deleted parameter is gone. Changing the keypair invalidates every access
  token at once; refresh tokens are DB-backed hashes and survive, so clients
  recover within one refresh cycle.
- Caddy's ACME account and certificates live on `/data/caddy`. Losing the data
  volume means re-issuing certificates, which is subject to Let's Encrypt rate
  limits.
