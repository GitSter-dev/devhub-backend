# 1. Realtime state stays on one instance, behind interfaces

Date: 2026-09-27. Status: accepted.

## Context

The backend runs as a single instance. Several pieces of state live in its memory:
- open WebSocket sessions (`RealtimeSessionRegistry`);
- the STOMP broker that fans messages out to them (Spring's simple broker);
- rate-limit buckets (Bucket4j on Caffeine).

V2 adds presence (online / last active), which is more of the same.

A second instance would not see the first one's state. Messages published on one would
not reach sockets held by the other, and each would count rate limits on its own.

## Decision

Keep one instance and in-memory state for now. Do not add Redis or an external broker
until a second instance is actually needed.

Build new realtime state behind an interface: presence first, then anything else that
must be shared. The in-memory implementation stays the default. Callers never touch the
maps or the broker directly.

## Consequences

- No extra service to run, secure, back up or pay for while traffic fits one machine.
  The metrics in `deploy/OBSERVABILITY.md` show when it stops fitting: connections,
  latency, CPU and heap.
- Scaling out is a known, bounded job. The work is:
  - Redis implementations of the presence and rate-limit interfaces;
  - a STOMP broker relay (RabbitMQ or ActiveMQ) in place of the simple broker;
  - sticky sessions, or a shared session registry, behind Caddy.

  None of it changes callers.
- Until then, the backend cannot scale horizontally. A restart drops every WebSocket;
  clients already reconnect and fill message gaps by sequence number.
