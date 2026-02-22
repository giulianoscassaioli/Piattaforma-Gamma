# Piattaforma Gamma - Architettura

## Cos'è

POC per il flusso PEC → Firma → Conservazione.

Ogni utente ha una casella PEC, arrivano allegati (fatture ecc.), l'utente li firma e li conserva. Il tutto è multi-tenant: ogni cliente vede solo i suoi dati.

---

## Struttura

Due microservizi Spring Boot completamente indipendenti:

- **pec-service** (8081) — gestisce caselle PEC, messaggi, allegati, conservazione
- **firma-service** (8082) — gestisce solo la firma

Non si chiamano mai direttamente tra loro, comunicano esclusivamente tramite Kafka. Il flusso è:

```
utente chiama GET /leggi-messaggi  (pec-service)
  → chiama MockPecApi per simulare arrivo messaggi
  → salva MessaggioPec in DB
  → ritorna [{id (UUID), messageId, oggetto, mittente}]

utente chiama GET /allegati/{allegatoId}/leggi-allegato  (pec-service)
  → segna l'allegato come letto (letto=true)
  → ritorna {id, filename}

utente chiama POST /firma/{id}/conferma  (firma-service)
  → salva record firma nel DB
  → [successo] pubblica FirmaRiuscitaEvent su "firma-riuscita-event"
  → [errore]   pubblica FirmaFallitaEvent  su "firma-fallita-event"

pec-service consumer riceve FirmaRiuscitaEvent
  → valida tenantId + userId
  → aggiorna allegato.firmato = true
  → chiama mock conservazione

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
                firmato, letto
```

Eliminando una casella vengono eliminati automaticamente a cascata tutti i suoi messaggi e i relativi allegati.

---

## Idempotenza import

- `leggi-messaggi`: se il messaggio mock è già in DB (stessa `message_id` + `casella_pec_id`) non crea duplicati.
- `leggi-allegati`: se l'allegato è già in DB (stesso `filename` del messaggio) non crea duplicati.

Le due operazioni sono separate per permettere all'utente di scegliere quale messaggio importare.

---

## Stack

- Java 25, Spring Boot 4.0.1
- PostgreSQL 16
- Redpanda (Kafka-compatible per sviluppo locale)
- Keycloak 26 per autenticazione OAuth2/JWT
- ELK Stack: Elasticsearch 8.12 + Logstash 8.12 + Kibana 8.12

Per la security ho usato Spring OAuth2 Resource Server che valida i JWT di Keycloak tramite il JWK endpoint, senza scrivere logica di validazione a mano.

## Multi-tenancy

Un filtro Spring (`TenantFilter`) legge `tenant_id` e `sub` dal JWT ad ogni richiesta e li mette in un ThreadLocal. I service li leggono da lì.

Per i consumer Kafka non c'è JWT quindi tenantId e userId viaggiano nel payload dell'evento e vengono validati prima di aggiornare qualcosa.

---

## Ruoli

| Ruolo   | `GET /api/caselle-pec`      | `GET /allegati/firmati`               |
|---------|-----------------------------|---------------------------------------|
| `user`  | Solo le proprie caselle     | Solo i propri allegati firmati        |
| `admin` | Tutte le caselle del tenant | Tutti gli allegati firmati del tenant |

Per tutte le altre operazioni (registra, elimina, leggi-messaggi, leggi-allegati) entrambi i ruoli hanno accesso — il filtro è solo sul `tenantId`.

---

## Mock

Il POC non si connette a sistemi esterni reali:

- **MockPecApi** — restituisce sempre gli stessi 2 messaggi hardcodati con IDs fissi (`msg-001`, `msg-002`). In produzione si userebbero le API del provider PEC.
- **MockConservazioneApi** — fa solo un log. In produzione upload S3 + chiamata API di conservazione reale.

---
