#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

KEYS_DIR=src/main/resources/keys

if [[ -f "$KEYS_DIR/private.pem" && -f "$KEYS_DIR/public.pem" ]]; then
  exit 0
fi

mkdir -p "$KEYS_DIR"
openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEYS_DIR/private.pem"
openssl pkey -in "$KEYS_DIR/private.pem" -pubout -out "$KEYS_DIR/public.pem"
chmod 600 "$KEYS_DIR/private.pem"
echo "Generated JWT key pair in $KEYS_DIR"
