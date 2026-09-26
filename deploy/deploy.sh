#!/usr/bin/env bash
set -euo pipefail

cd /opt/devhub
: "${IMAGE_TAG:?IMAGE_TAG is not set}"

REGION="${AWS_REGION:-eu-north-1}"
COMPOSE=(docker compose -f compose.prod.yaml --env-file app.env --env-file image.env)
PREVIOUS=$(cat current_tag 2>/dev/null || true)
CONSOLE_TAG=$(cat console_tag 2>/dev/null || true)

echo "==> refreshing secrets"
./render-env.sh

echo "==> logging in to ECR"
registry=$(grep '^ECR_REPOSITORY_URL=' app.env | cut -d= -f2- | cut -d/ -f1)
aws ecr get-login-password --region "$REGION" |
	docker login --username AWS --password-stdin "$registry" >/dev/null

roll() {
	./write-image-env.sh "$1" "$CONSOLE_TAG"
	"${COMPOSE[@]}" pull -q app
	"${COMPOSE[@]}" up -d --wait --wait-timeout 300
}

# Caddy bind-mounts the single Caddyfile. A deploy replaces that file with a new
# inode, which a running container never sees, so restart Caddy when it changed.
reload_caddy_if_changed() {
	local sum
	sum=$(sha256sum Caddyfile | cut -d' ' -f1)
	if [ "$sum" != "$(cat caddyfile_sum 2>/dev/null || true)" ]; then
		echo "==> Caddyfile changed, validating and restarting caddy"
		"${COMPOSE[@]}" run --rm --no-deps caddy caddy validate --config /etc/caddy/Caddyfile >/dev/null || return 1
		"${COMPOSE[@]}" restart caddy || return 1
		echo "$sum" >caddyfile_sum
	fi
}

healthy() {
	curl -fsS --max-time 5 --retry 12 --retry-delay 5 --retry-all-errors \
		http://127.0.0.1:8080/actuator/health/readiness >/dev/null
}

echo "==> rolling to $IMAGE_TAG"
if roll "$IMAGE_TAG" && healthy; then
	echo "$IMAGE_TAG" >current_tag
	if ! reload_caddy_if_changed; then
		echo "==> new Caddyfile is invalid; caddy keeps the previous config" >&2
		exit 1
	fi
	docker image prune -f >/dev/null
	echo "==> deployed $IMAGE_TAG"
	exit 0
fi

echo "==> health gate failed for $IMAGE_TAG" >&2
if [ -n "$PREVIOUS" ] && [ "$PREVIOUS" != "$IMAGE_TAG" ]; then
	echo "==> rolling back to $PREVIOUS" >&2
	roll "$PREVIOUS" || true
	healthy && echo "==> rolled back to $PREVIOUS" >&2
fi
exit 1
