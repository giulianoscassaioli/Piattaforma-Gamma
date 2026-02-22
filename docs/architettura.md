# Piattaforma Gamma - Architettura

## Cos'è

POC per il flusso PEC → Firma → Conservazione.

Ogni utente ha una casella PEC, arrivano allegati (fatture ecc.), l'utente li firma e li conserva. Il tutto è multi-tenant: ogni cliente vede solo i suoi dati.

---

## Struttura

Due microservizi Spring Boot completamente indipendenti:

- **pec-service** (8081) — gestisce caselle PEC, allegati, conservazione
- **firma-service** (8082) — gestisce solo la firma

Non si chiamano mai direttamente tra loro, comunicano esclusivamente tramite Kafka. Il flusso è:

```
utente chiama POST /leggi-allegati  (pec-service)
  → salva allegati nel DB con firmato=false
  → ritorna [{id, filename}] per ogni allegato importato

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

## Stack

- Java 25, Spring Boot 4.0.1
- PostgreSQL 16
- Redpanda (Kafka-compatible per sviluppo locale)
- Keycloak 26 per autenticazione OAuth2/JWT
- ELK Stack Elasticsearch 8.12 + Logstash 8.12 + Kibana 8.12

Per la security ho usato Spring OAuth2 Resource Server che valida i JWT di Keycloak tramite il JWK endpoint, senza scrivere logica di validazione a mano.

## Multi-tenancy

Un filtro Spring (`TenantFilter`) legge `tenant_id` e `sub` dal JWT ad ogni richiesta e li mette in un ThreadLocal. I service li leggono da lì.

Per i consumer Kafka non c'è JWT quindi tenantId e userId viaggiano nel payload dell'evento e vengono validati prima di aggiornare qualcosa.

---

## Mock

Il POC non si connette a sistemi esterni reali:

- **MockPecApi** — restituisce sempre gli stessi 2 messaggi hardcodati. In produzione si userebbero le API del provider PEC.
- **MockConservazioneApi** — fa solo un log. In produzione upload S3 + chiamata API di conservazione reale.

---
