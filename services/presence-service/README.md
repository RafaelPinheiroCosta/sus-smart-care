# presence-service

Porta local: `8086`.

Estrutura alvo: domain / application / adapters / infrastructure.

## Advanced presence tracking

Presence is technology-neutral.

Supported sources are BLE, WIFI, UWB, QR, KIOSK, MANUAL and SYSTEM.

A PresenceTrackingSession is temporal and linked to a visit. The API returns
an opaque tracking token when the session starts. Only the SHA-256 hash of
that token is persisted.

Raw PresenceSignal records contain the tracking-session reference and signal
metadata. They do not persist patientId or visitId.

Physical signal sources must identify a registered gateway. Manual and system
signals support patients without smartphones.

The current zone state is maintained separately from the append-only raw
signals. Operational zone queries expose occupancy counts and bottleneck
status without exposing patient identity.

Clinical priority is never changed by presence tracking.