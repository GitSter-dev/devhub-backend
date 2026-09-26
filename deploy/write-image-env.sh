#!/usr/bin/env bash
set -euo pipefail

# image.env pins both images. The console runs under a compose profile that is
# only switched on once it has a tag, so a host without a console still starts.
backend_tag="$1"
console_tag="${2:-}"

{
	echo "IMAGE_TAG=$backend_tag"
	if [ -n "$console_tag" ]; then
		echo "CONSOLE_IMAGE_TAG=$console_tag"
		echo "COMPOSE_PROFILES=console"
	fi
} >/opt/devhub/image.env
