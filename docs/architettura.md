# Piattaforma Gamma - Architettura

## Cos'è

POC per il flusso PEC → Firma → Conservazione.

Ogni utente ha una casella PEC, arrivano allegati (fatture ecc.), l'utente li firma e li conserva. Il tutto è multi-tenant: ogni cliente vede solo i suoi dati.

---

## Struttura

Tre moduli Spring Boot:

- **gateway-service** (8090) — unico punto di ingresso, valida il JWT e instrada le richieste
- **pec-service** (8081) — gestisce caselle PEC, messaggi, allegati, conservazione
- **firma-service** (8082) — gestisce solo la firma

```
Client
  │  Authorization: Bearer <JWT>
  ▼
gateway-service:8090
  │  valida JWT con Keycloak
  │  estrae tenant_id, sub, ruoli dal JWT
  │  propaga X-Tenant-Id / X-User-Id / X-Roles
  ├─ /api/caselle-pec/** ──→ pec-service:8081
  └─ /api/firma/**       ──→ firma-service:8082
```

pec-service e firma-service non si chiamano mai direttamente tra loro: comunicano solo tramite Kafka.

---

## Autenticazione e Propagazione del Tenant

### Gateway

Il gateway valida il JWT emesso da Keycloak tramite Spring OAuth2 Resource Server (verifica firma con JWK endpoint, cachato localmente). Se il token è invalido o assente risponde `401` senza toccare i microservizi.

Dopo la validazione, il `ProxyController` estrae dal JWT:
- `tenant_id` → header `X-Tenant-Id`
- `sub` → header `X-User-Id`
- `realm_access.roles` → header `X-Roles` (es. `"user,admin"`)

Questi header vengono aggiunti alla richiesta inoltrata ai microservizi.

### pec-service / firma-service

I microservizi leggono i tre header `X-*` e:
1. Popolano il `TenantContext` (ThreadLocal) con `tenantId` e `userId` — usato dai service per filtrare i dati
2. Creano un `UsernamePasswordAuthenticationToken` con le authority `ROLE_user`, `ROLE_admin` — usato da Spring Security per `@PreAuthorize` e `.authenticated()`

Per i consumer Kafka non c'è JWT: `tenantId` e `userId` viaggiano nel payload dell'evento e vengono validati prima di aggiornare qualcosa.

---

## Flusso richiesta completo

```
utente chiama GET /api/caselle-pec/{id}/leggi-messaggi  (gateway:8090)
  → gateway verifica JWT con Keycloak
  → gateway aggiunge X-Tenant-Id / X-User-Id / X-Roles
  → gateway inoltra a pec-service:8081
     → TenantFilter legge gli header, popola TenantContext
     → controller esegue, filtra per tenantId/userId

utente chiama POST /api/firma/{id}/conferma  (gateway:8090)
  → gateway verifica JWT, propaga header
  → gateway inoltra a firma-service:8082
     → salva record firma nel DB
     → [successo] pubblica FirmaRiuscitaEvent su "firma-riuscita-event"
     → [errore]   pubblica FirmaFallitaEvent  su "firma-fallita-event"

pec-service consumer riceve FirmaRiuscitaEvent
  → valida tenantId + userId dall'evento
  → aggiorna allegato.firmato = true, allegato.conservato = true
  → carica il file su S3 (bucket: gamma-allegati, key: filename)

pec-service consumer riceve FirmaFallitaEvent
  → valida tenantId + userId
  → se allegato era già firmato, riporta firmato = false (rollback)
```

---

## Relazioni Entità (pec-service)

```
casella_pec
  │  id (UUID)
  │  tenant_id, user_id   ← stringhe dal JWT
  │  indirizzo
  │
  └─── messaggio_pec  (OneToMany, cascade ALL, orphanRemoval)
         │  id (UUID)
         │  message_id
         │  oggetto, mittente
         │
         └─── allegato  (OneToMany, cascade ALL, orphanRemoval)
                id (UUID)
                tenant_id, user_id
                filename
                firmato, conservato, letto
```

Eliminando una casella vengono eliminati automaticamente a cascata tutti i suoi messaggi e i relativi allegati.

---

## Stack

- Java 25, Spring Boot 4.0.1
- PostgreSQL 16
- Redpanda (Kafka-compatible per sviluppo locale)
- Keycloak 26 per autenticazione OAuth2/JWT
- MinIO — object storage S3-compatible per gli allegati firmati
- ELK Stack: Elasticsearch 8.12 + Logstash 8.12 + Kibana 8.12

> Spring Cloud Gateway (WebFlux e MVC) non è compatibile con Spring Boot 4.0.1 — il routing del gateway è implementato con `RestTemplate` + `@RestController`.

---

## Multi-tenancy

Il `TenantFilter` in ogni microservizio legge gli header `X-Tenant-Id` e `X-User-Id` propagati dal gateway e li mette in un ThreadLocal (`TenantContext`). I service li leggono da lì per filtrare le query.

Per i consumer Kafka, `tenantId` e `userId` viaggiano nel payload dell'evento e vengono validati prima di aggiornare qualcosa.

---

## Ruoli

| Ruolo   | `GET /api/caselle-pec`      | `GET /allegati/firmati`               |
|---------|-----------------------------|---------------------------------------|
| `user`  | Solo le proprie caselle     | Solo i propri allegati firmati        |
| `admin` | Tutte le caselle del tenant | Tutti gli allegati firmati del tenant |

Per tutte le altre operazioni (registra, elimina, leggi-messaggi, leggi-allegati) entrambi i ruoli hanno accesso — il filtro è solo sul `tenantId`.

---

## Mock

- **MockPecApi** — Simula ricezione messaggi.
