# Piattaforma Gamma

POC locale per la gestione del flusso **PEC → Firma Digitale → Conservazione**.

Architettura **event-driven** con Kafka: i microservizi non si chiamano mai direttamente, comunicano solo tramite AllegatoFirmatoEvent.

Per le scelte architetturali e tecnologiche vedere [docs/architettura.md](docs/architettura.md).

---

## Prerequisiti

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) avviato
- Java 25
- Maven 3.9+

---

## Avvio

### 1. Compila tutti i moduli

```bash
mvn clean install -DskipTests
```

### 2. Avvia l'infrastruttura

```bash
docker compose up -d
```

Quando tutti i container sono healthy:

```bash
docker compose ps
```

| Servizio         | URL                   | Note                                 |
|------------------|-----------------------|--------------------------------------|
| PostgreSQL       | `localhost:5432`      | DB: `gamma`, User: `admin`           |
| pgAdmin          | http://localhost:5050 | Login: `giuliano@email.it` / `admin` |
| Redpanda         | `localhost:9092`      | Broker Kafka-compatible              |
| Redpanda Console | http://localhost:8083 | UI topic e consumer group            |
| Keycloak         | http://localhost:8080 | Identity Provider (realm: `gamma`)   |
| Grafana          | http://localhost:3000 | Dashboard log (Loki)                 |

### 3. Avvia i microservizi

In due terminali separati:

```bash
# Terminale 1 — pec-service (porta 8081)
cd pec-service && mvn spring-boot:run
```

```bash
# Terminale 2 — firma-service (porta 8082)
cd firma-service && mvn spring-boot:run
```

---

## Utenti di test

Gli utenti sono preconfigurati nel realm export (`keycloak/realm-export.json`) e vengono importati automaticamente all'avvio di Keycloak.

| Username | Password    | tenant_id | Ruoli        |
|----------|-------------|-----------|--------------|
| user1    | password123 | tenant-1  | user         |
| user2    | password123 | tenant-2  | user         |
| admin1   | admin123    | tenant-1  | user, admin  |

---

## Ottenere un token JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/gamma/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=gamma-client&username=user1&password=password123" \
  | jq -r .access_token)
```

> Fetcha il token delle chiamate tramite oauth2 di Bruno.

---

## API

Tutte le chiamate richiedono l'header `Authorization: Bearer <token>`.

### pec-service — `http://localhost:8081`

| Metodo | Path                                  | Descrizione                                                    |
|--------|---------------------------------------|----------------------------------------------------------------|
| GET    | `/api/caselle-pec`                    | Lista caselle con stato (user: proprie, admin: tutto il tenant) |
| POST   | `/api/caselle-pec`                    | Registra una casella PEC                                       |
| DELETE | `/api/caselle-pec/{id}`               | Elimina casella (cancella anche i suoi allegati)               |
| POST   | `/api/caselle-pec/{id}/leggi-allegati`| Importa allegati dai messaggi mock → ritorna lista con UUID    |
| GET    | `/api/caselle-pec/allegati/firmati`   | Allegati firmati (user: propri, admin: tutto il tenant)        |

### firma-service — `http://localhost:8082`

| Metodo | Path                               | Descrizione                |
|--------|------------------------------------|----------------------------|
| POST   | `/api/firma/{allegatoId}/conferma` | Firma un allegato per UUID |

---

## Ruoli

| Ruolo   | GET /api/caselle-pec              | GET /allegati/firmati                   |
|---------|-----------------------------------|-----------------------------------------|
| `user`  | Solo le proprie caselle           | Solo i propri allegati firmati          |
| `admin` | Tutte le caselle del tenant       | Tutti gli allegati firmati del tenant   |

---

## Flusso completo

```
1. POST /api/caselle-pec/{id}/leggi-allegati   (pec-service)
        │  legge messaggi mock dalla casella PEC
        │  salva allegati nel DB (firmato=false)
        └→ ritorna: [{ id, filename }, ...]

2. Per ogni allegato da firmare:
   POST /api/firma/{allegatoId}/conferma  (firma-service)
        │  crea record allegato_firma (allegatoId, tenantId, userId, firmatoAt)
        └→ pubblica AllegatoFirmatoEvent su topic "allegato-firmato"

3. pec-service riceve AllegatoFirmatoEvent
        │  valida tenantId + userId
        │  aggiorna allegato.firmato = true
        └→ invia a mock conservazione (log)

4. GET /api/caselle-pec  (pec-service)
        └→ stato casella: IN_ATTESA_FIRMA o CONSERVATA (calcolato dagli allegati)
```

### Kafka topics

| Topic             | Produttore    | Consumatore | Consumer Group      |
|-------------------|---------------|-------------|---------------------|
| `allegato-firmato`| firma-service | pec-service | `pec-conservazione` |

---

## Tabelle nel DB

Hibernate crea le tabelle automaticamente al primo avvio (`ddl-auto: update`).

| Tabella         | Servizio      | Descrizione                                                         |
|-----------------|---------------|---------------------------------------------------------------------|
| `casella_pec`   | pec-service   | Caselle PEC registrate (tenant_id, user_id, indirizzo)              |
| `allegato`      | pec-service   | Allegati estratti dai messaggi (tenant_id, user_id, firmato)        |
| `allegato_firma`| firma-service | Storico firme effettuate (allegato_id, tenant_id, user_id, firmato_at) |

> Eliminando una casella vengono eliminati automaticamente anche tutti i suoi allegati (cascade).

---

## Connessione a giu di pgAdmin

1. Vai su http://localhost:5050
2. Login: `giuliano@email.it` / `admin`
3. Click destro su **Servers** → **Register → Server**
   - **Name**: `gamma`
   - Tab **Connection**: Host `postgres`, Port `5432`, Database `gamma`, Username `admin`, Password `admin`

---

## Fermare tutto

```bash
docker compose down -v
```
