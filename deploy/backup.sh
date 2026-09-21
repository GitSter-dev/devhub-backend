#!/usr/bin/env bash
set -euo pipefail

cd /opt/devhub

REGION="${AWS_REGION:-eu-north-1}"
BACKUP_DIR=/data/backups
RETAIN_DAYS=7

DB_NAME=$(grep '^DB_NAME=' app.env | cut -d= -f2-)
DB_USER=$(grep '^DB_USER=' app.env | cut -d= -f2-)
BUCKET=$(grep '^BACKUP_BUCKET=' app.env | cut -d= -f2-)

used=$(df --output=pcent /data | tail -1 | tr -dc '0-9')
if [ "$used" -ge 80 ]; then
	echo "/data is ${used}% full, refusing to write a backup" >&2
	exit 1
fi

mkdir -p "$BACKUP_DIR"
stamp=$(date -u +%Y%m%dT%H%M%SZ)
file="$BACKUP_DIR/devhub-$stamp.dump"

docker compose -f compose.prod.yaml --env-file app.env --env-file image.env \
	exec -T postgres pg_dump -U "$DB_USER" -d "$DB_NAME" --format=custom >"$file"

if [ ! -s "$file" ]; then
	echo "pg_dump produced an empty file" >&2
	rm -f "$file"
	exit 1
fi

aws s3 cp "$file" "s3://$BUCKET/postgres/devhub-$stamp.dump" --region "$REGION"
find "$BACKUP_DIR" -name 'devhub-*.dump' -mtime "+$RETAIN_DAYS" -delete

echo "backed up $(du -h "$file" | cut -f1) to s3://$BUCKET/postgres/devhub-$stamp.dump"
