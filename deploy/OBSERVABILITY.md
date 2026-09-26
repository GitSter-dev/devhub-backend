# Metrics and traces

The backend sends metrics and traces over OTLP. Locally they go to the Grafana stack
in `compose.yaml`; in production they go to Grafana Cloud's free tier, so monitoring
costs the server no memory. Export is off unless `OTEL_EXPORT_ENABLED=true`.

| What | Where it comes from |
|---|---|
| Requests, latency, status codes | Spring MVC (`http.server.requests`, with SLO latency buckets) |
| JVM memory and CPU, DB pool | Spring Boot and HikariCP |
| Realtime connections, outbox, push, rate limits, outdated apps | `DevHubMetrics` (`devhub.*`) |
| Traces | Every request and scheduled job, sampled at 10% in production |

Health-check calls to `/actuator` are not recorded.

## Locally

`./start.sh` starts Grafana with the rest of the stack. The `.env.example` settings
already export there, tracing every request and pushing metrics every 15 seconds.

- Grafana: <http://localhost:3000>. The **DevHub → DevHub backend** dashboard is loaded
  automatically.
- Traces: Explore → Tempo, then search for service `devhub-backend`.

## Production (Grafana Cloud)

1. Create a free stack at grafana.com. Under **Connections → OpenTelemetry (OTLP)**,
   generate a token. Note the OTLP endpoint (for example
   `https://otlp-gateway-prod-eu-west-2.grafana.net/otlp`) and the instance ID.
2. Store the settings in Parameter Store. Only the token is secret:

   ```bash
   P=/devhub/prod; R=eu-north-1
   aws ssm put-parameter --region $R --type String --overwrite --name $P/OTEL_EXPORT_ENABLED --value true
   aws ssm put-parameter --region $R --type String --overwrite --name $P/OTLP_ENDPOINT \
     --value https://otlp-gateway-<region>.grafana.net/otlp
   aws ssm put-parameter --region $R --type SecureString --overwrite --name $P/OTLP_AUTHORIZATION \
     --value "Basic $(printf '%s:%s' <instance-id> <token> | base64 -w0)"
   ```

3. Re-run the **deploy** workflow with the current `image_tag`, so the instance picks up
   the new environment.
4. In Grafana Cloud, go to Dashboards → New → Import, and upload
   `observability/dashboards/devhub-backend.json`. Pick the stack's Prometheus data source.

Every series carries `service_version` (the image tag) and
`deployment_environment_name`, so a regression can be traced to the deploy that caused it.

## Staying inside the free tier

The free tier allows 10k active metric series. The backend uses a few hundred:
- latency uses seven fixed buckets rather than full histograms;
- every `devhub.*` tag comes from a small fixed set, never user IDs or free text.

Before adding a metric, check that its tags stay bounded. To cut volume, lower
`TRACING_SAMPLING_PROBABILITY` or raise `OTLP_METRICS_STEP` (default `1m`).
