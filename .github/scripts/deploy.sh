#!/usr/bin/env bash
set -euo pipefail

: "${IMAGE_TAG:?}"
: "${INSTANCE_ID:?}"
: "${AWS_REGION:?}"

# The runtime topology lives in the repo, so ship the current version before
# rolling. It carries no secrets: those are read from Parameter Store by the
# instance role.
BUNDLE=$(tar -czf - -C deploy compose.prod.yaml Caddyfile deploy.sh deploy-console.sh write-image-env.sh render-env.sh backup.sh | base64 -w0)

command_id=$(aws ssm send-command \
	--region "$AWS_REGION" \
	--instance-ids "$INSTANCE_ID" \
	--document-name AWS-RunShellScript \
	--comment "deploy $IMAGE_TAG" \
	--timeout-seconds 600 \
	--parameters "commands=[
		'set -euo pipefail',
		'echo $BUNDLE | base64 -d | tar -xz -C /opt/devhub',
		'chmod +x /opt/devhub/*.sh',
		'AWS_REGION=$AWS_REGION IMAGE_TAG=$IMAGE_TAG /opt/devhub/deploy.sh'
	]" \
	--query Command.CommandId --output text)

echo "ssm command: $command_id"

# The built-in `ssm wait command-executed` gives up after 20 attempts (100s),
# which is shorter than a normal deploy.
status=Pending
for _ in $(seq 1 120); do
	status=$(aws ssm get-command-invocation \
		--region "$AWS_REGION" \
		--command-id "$command_id" \
		--instance-id "$INSTANCE_ID" \
		--query Status --output text 2>/dev/null || echo Pending)
	case "$status" in
	Success | Failed | Cancelled | TimedOut) break ;;
	esac
	sleep 5
done

aws ssm get-command-invocation --region "$AWS_REGION" \
	--command-id "$command_id" --instance-id "$INSTANCE_ID" \
	--query StandardOutputContent --output text

aws ssm get-command-invocation --region "$AWS_REGION" \
	--command-id "$command_id" --instance-id "$INSTANCE_ID" \
	--query StandardErrorContent --output text >&2

if [ "$status" != "Success" ]; then
	echo "::error::deploy finished with status $status"
	exit 1
fi

echo "deployed $IMAGE_TAG"
