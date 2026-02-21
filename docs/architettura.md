# Piattaforma Gamma - Architettura

## Cos'è

POC per il flusso PEC → Firma → Conservazione.

Ogni utente ha una casella PEC, arrivano allegati (fatture ecc.), l'utente li firma e manda e li conserva. Il tutto deve essere multi-tenant quindi ogni cliente vede solo i suoi dati.

---

## Struttura

Due microservizi Spring Boot:

- **pec-service** (8081) - gestisce caselle PEC, allegati, conservazione
- **firma-service** (8082) - gestisce solo la firma

Non si chiamano mai direttamente tra loro, usano Kafka. Il flusso è:

```
utente chiama POST /leggi-allegati (pec-service)
  → salva allegati nel DB con firmato=false

utente chiama POST /firma/{id}/conferma (firma-service)
  → salva record firma
  → pubblica evento su topic "allegato-firmato"

pec-service consumer riceve l'evento
  → aggiorna allegato.firmato = true
  → chiama mock conservazione
```

## Stack

- Java 25, Spring Boot 4.0.1
- PostgreSQL 16
- Redpanda (Kafka per sviluppo locale)
- Keycloak 26 per autenticazione OAuth2/JWT
- Loki + Grafana x logging

Per la security ho usato Spring OAuth2 Resource Server che valida i JWT di Keycloak tramite il JWK endpoint, senza scrivere logica di validazione a mano.

---

## Multi-tenancy

Un filtro Spring (`TenantFilter`) legge `tenant_id` e `sub` dal JWT ad ogni richiesta e li mette in un ThreadLocal. I service li leggono da lì.

Per i consumer Kafka non c'è JWT quindi tenantId e userId viaggiano nel payload dell'evento e vengono validati prima di aggiornare qualcosa.

---

## Mock

Il POC non si connette a sistemi esterni reali:

- **MockPecApi** - restituisce sempre gli stessi 2 messaggi hardcodati. In produzione si userebbero le API del provider PEC
- **MockConservazioneApi** - fa solo un log. In produzione upload S3 + chiamata API di conservazione reale

---
