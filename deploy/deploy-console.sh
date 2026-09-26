#!/usr/bin/env bash
set -euo pipefail

cd /opt/devhub
CONSOLE_IMAGE_TAG="${1:?usage: deploy-console.sh <image-tag>}"

REGION="${AWS_REGION:-eu-north-1}"
COMPOSE=(docker compose -f compose.prod.yaml --env-file app.env --env-file image.env)
BACKEND=$(cat current_tag 2>/dev/null || true)
PREVIOUS=$(cat console_tag 2>/dev/null || true)

if [ -z "$BACKEND" ]; then
	echo "no backend deployed yet; deploy the backend first" >&2
	exit 1
fi

echo "==> refreshing configuration"
./render-env.sh

echo "==> logging in to ECR"
registry=$(grep '^CONSOLE_ECR_REPOSITORY_URL=' app.env | cut -d= -f2- | cut -d/ -f1)
aws ecr get-login-password --region "$REGION" |
	docker login --username AWS --password-stdin "$registry" >/dev/null

roll() {
	./write-image-env.sh "$BACKEND" "$1"
	"${COMPOSE[@]}" pull -q console
	"${COMPOSE[@]}" up -d --wait --wait-timeout 120 console
}

echo "==> rolling console to $CONSOLE_IMAGE_TAG"
if roll "$CONSOLE_IMAGE_TAG"; then
	echo "$CONSOLE_IMAGE_TAG" >console_tag
	docker image prune -f >/dev/null
	echo "==> deployed console $CONSOLE_IMAGE_TAG"
	exit 0
fi

echo "==> console failed its health check" >&2
if [ -n "$PREVIOUS" ] && [ "$PREVIOUS" != "$CONSOLE_IMAGE_TAG" ]; then
	echo "==> rolling console back to $PREVIOUS" >&2
	roll "$PREVIOUS" && echo "==> rolled console back to $PREVIOUS" >&2
else
	./write-image-env.sh "$BACKEND" "$PREVIOUS"
fi
exit 1
