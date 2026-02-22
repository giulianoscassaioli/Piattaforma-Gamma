# Piattaforma Gamma

POC locale per la gestione del flusso **PEC → Firma Digitale → Conservazione**.

Architettura **event-driven** con Kafka. Per le scelte architetturali vedere [docs/architettura.md](docs/architettura.md).

---

## Prerequisiti

- Docker
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

| Servizio         | URL                   | Note                                    |
|------------------|-----------------------|-----------------------------------------|
| PostgreSQL       | `localhost:5432`      | DB: `gamma`, User: `admin`              |
| pgAdmin          | http://localhost:5050 | Login: `giuliano@email.it` / `admin`    |
| Redpanda         | `localhost:9092`      | Broker Kafka-compatible                 |
| Redpanda Console | http://localhost:8083 | UI topic e consumer group               |
| Keycloak         | http://localhost:8080 | Identity Provider (realm: `gamma`)      |
| Elasticsearch    | http://localhost:9200 | Storage e ricerca log                   |
| Logstash         | `localhost:5044`      | Ingestion log via TCP (JSON)            |
| Kibana           | http://localhost:5601 | Dashboard log                           |

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

## Visualizzare i log con Kibana

I log di entrambi i servizi vengono inviati automaticamente a Logstash e indicizzati su Elasticsearch. Al primo avvio occorre configurare il Data View su Kibana.

### Configurazione iniziale (una volta sola)

1. Avvia i microservizi e fai almeno una chiamata API per generare i primi log
2. Apri Kibana su **http://localhost:5601**
3. Clicca sull'icona del menu hamburger in alto a sinistra
4. Vai su **Stack Management** → **Data Views**
5. Clicca **Create data view**
6. Compila i campi:
   - **Name:** `logs`
   - **Index pattern:** `gamma-logs-*`
   - **Timestamp field:** `@timestamp`
7. Clicca **Save data view to Kibana**

### Esplorare i log

1. Menu hamburger → **Analytics** → **Discover**
2. Seleziona il data view **logs** in alto a sinistra
3. Regola il range temporale in alto a destra (es. **Last 15 minutes**)
4. I log appaiono in ordine cronologico con tutti i campi JSON espansi

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

> In alternativa usa il flow OAuth2 integrato in Bruno o Postman.

---

## API

Tutte le chiamate richiedono l'header `Authorization: Bearer <token>`.

### pec-service — `http://localhost:8081`

| Metodo | Path                                   | Descrizione                                                     |
|--------|----------------------------------------|-----------------------------------------------------------------|
| GET    | `/api/caselle-pec`                     | Lista caselle con messaggi e allegati (user: proprie, admin: tutto il tenant) |
| POST   | `/api/caselle-pec`                     | Registra una casella PEC                                                       |
| DELETE | `/api/caselle-pec/{id}`                | Elimina casella (cancella anche i suoi allegati)                               |
| POST   | `/api/caselle-pec/{id}/leggi-allegati` | Importa allegati dai messaggi mock → ritorna `[{id, filename}]`                |
| GET    | `/api/caselle-pec/allegati/firmati`    | Allegati firmati (user: propri, admin: tutto il tenant)                        |

### firma-service — `http://localhost:8082`

| Metodo | Path                               | Descrizione                |
|--------|------------------------------------|----------------------------|
| POST   | `/api/firma/{allegatoId}/conferma` | Firma un allegato per UUID |

---

## Ruoli

| Ruolo   | GET /api/caselle-pec        | GET /allegati/firmati                 |
|---------|-----------------------------|---------------------------------------|
| `user`  | Solo le proprie caselle     | Solo i propri allegati firmati        |
| `admin` | Tutte le caselle del tenant | Tutti gli allegati firmati del tenant |

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
        ├→ [successo] pubblica FirmaRiuscitaEvent su topic "firma-riuscita-event"
        └→ [errore]   pubblica FirmaFallitaEvent  su topic "firma-fallita-event"

3a. pec-service riceve FirmaRiuscitaEvent
        │  valida tenantId + userId
        │  aggiorna allegato.firmato = true
        └→ invia a mock conservazione (log)

3b. pec-service riceve FirmaFallitaEvent
        │  valida tenantId + userId
        └→ se allegato.firmato=true, riporta firmato=false (rollback)

4. GET /api/caselle-pec  (pec-service)
        └→ ritorna lista caselle con messaggi e per ogni allegato: { id, filename, letto, firmato }
```

### Kafka topics

| Topic                  | Produttore    | Consumatore | Consumer Group | Evento          |
|------------------------|---------------|-------------|----------------|-----------------|
| `firma-riuscita-event` | firma-service | pec-service | `pec-eventi`   | Firma riuscita  |
| `firma-fallita-event`  | firma-service | pec-service | `pec-eventi`   | Firma fallita   |

---

## Tabelle nel DB

Hibernate crea le tabelle automaticamente al primo avvio (`ddl-auto: update`).

| Tabella            | Servizio      | Descrizione                                                            |
|--------------------|---------------|------------------------------------------------------------------------|
| `casella_pec`      | pec-service   | Caselle PEC registrate (tenant_id, user_id, indirizzo)                 |
| `allegato`         | pec-service   | Allegati estratti dai messaggi (tenant_id, user_id, firmato)           |
| `allegato_firmato` | firma-service | Storico firme effettuate (allegato_id, tenant_id, user_id, firmato_at) |

> Eliminando una casella vengono eliminati automaticamente anche tutti i suoi allegati (cascade).

---

## Connessione a pgAdmin

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
