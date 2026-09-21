#!/usr/bin/env bash
set -euo pipefail

REGION="${AWS_REGION:-eu-north-1}"
PARAM_PATH="${PARAM_PATH:-/devhub/prod}"
DIR=/opt/devhub
SECRETS="$DIR/secrets"

umask 077
mkdir -p "$SECRETS"
TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

aws ssm get-parameters-by-path \
	--region "$REGION" \
	--path "$PARAM_PATH" \
	--recursive \
	--with-decryption \
	--query 'Parameters[].{n:Name,v:Value}' \
	--output json >"$TMP"

count=$(jq 'length' "$TMP")
if [ "$count" -eq 0 ]; then
	echo "no parameters found under $PARAM_PATH" >&2
	exit 1
fi

extract() {
	jq -r --arg k "$1" '.[] | select(.n | endswith($k)) | .v' "$TMP"
}

extract JWT_PRIVATE_KEY >"$SECRETS/private.pem"
extract JWT_PUBLIC_KEY >"$SECRETS/public.pem"

firebase=$(extract FIREBASE_CREDENTIALS_JSON)
if [ -n "$firebase" ]; then
	printf '%s' "$firebase" >"$SECRETS/firebase.json"
fi

jq -r --arg p "$PARAM_PATH/" '
	.[]
	| select((.n | endswith("JWT_PRIVATE_KEY")
		or endswith("JWT_PUBLIC_KEY")
		or endswith("FIREBASE_CREDENTIALS_JSON")) | not)
	| "\(.n | ltrimstr($p))=\(.v)"
' "$TMP" >"$DIR/app.env"

# The app container runs as uid/gid 10001. The directory needs group search
# permission too, not just the files, or every read is a permission error.
chown root:10001 "$SECRETS"
chmod 0750 "$SECRETS"
chown root:10001 "$SECRETS"/*
chmod 0640 "$SECRETS"/*
chmod 0600 "$DIR/app.env"

echo "rendered $count parameters from $PARAM_PATH"
