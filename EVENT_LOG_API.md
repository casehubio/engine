# Event Log Query API

## Overview

REST API endpoint для получения событий (EventLog) конкретного case с возможностью фильтрации по типам событий и типам потоков, отсортированных по порядковому номеру (seq).

## Endpoint

```
GET /api/cases/{caseId}/events
```

### Path Parameters

- `caseId` (UUID, required) - идентификатор case

### Query Parameters

- `eventType` (string, optional, repeatable) - фильтр по типу события. Можно указать несколько значений.
  - Допустимые значения: `CASE_STARTED`, `CASE_COMPLETED`, `CASE_FAULTED`, `CASE_CANCELLED`, `CASE_STATUS_CHANGED`, `TASK_CREATED`, `TASK_COMPLETED`, `TASK_FAILED`, `TASK_CANCELLED`, `WORKER_SCHEDULED`, `WORKER_EXECUTION_STARTED`, `WORKER_EXECUTION_COMPLETED`, `WORKER_EXECUTION_FAILED`, `WORK_SUBMITTED`, `WORK_COMPLETED`, `SIGNAL_RECEIVED`, `MILESTONE_REACHED`, `MILESTONE_ACTIVATED`, `MILESTONE_COMPLETED`, `MILESTONE_SLA_VIOLATED`, `GOAL_REACHED`, `SUBCASE_STARTED`, `SUBCASE_COMPLETED`

- `streamType` (string, optional, repeatable) - фильтр по типу потока. Можно указать несколько значений.
  - Допустимые значения: `CASE`, `WORKER`, `TIMER`, `SYSTEM`

## Response

Массив объектов EventLogDTO, отсортированных по полю `seq` в порядке возрастания.

### EventLogDTO Structure

```json
{
  "id": 123,
  "caseId": "550e8400-e29b-41d4-a716-446655440000",
  "seq": 1,
  "eventType": "CASE_STARTED",
  "streamType": "CASE",
  "workerId": "worker-1",
  "timestamp": "2026-05-07T10:00:00Z",
  "payload": {},
  "metadata": {}
}
```

## Examples

### Получить все события case

```bash
curl http://localhost:8080/api/cases/550e8400-e29b-41d4-a716-446655440000/events
```

### Получить только события типа WORKER

```bash
curl "http://localhost:8080/api/cases/550e8400-e29b-41d4-a716-446655440000/events?streamType=WORKER"
```

### Получить события запуска и завершения worker'ов

```bash
curl "http://localhost:8080/api/cases/550e8400-e29b-41d4-a716-446655440000/events?eventType=WORKER_EXECUTION_STARTED&eventType=WORKER_EXECUTION_COMPLETED"
```

### Комбинированный фильтр: события CASE потока типа CASE_STARTED или CASE_COMPLETED

```bash
curl "http://localhost:8080/api/cases/550e8400-e29b-41d4-a716-446655440000/events?streamType=CASE&eventType=CASE_STARTED&eventType=CASE_COMPLETED"
```

## Implementation Details

### Repository Layer

Новый метод в `EventLogRepository`:

```java
Uni<List<EventLog>> findByCaseWithFilters(
    UUID caseId, 
    Collection<CaseHubEventType> eventTypes, 
    Collection<EventStreamType> streamTypes
);
```

- Если `eventTypes` или `streamTypes` равны `null` или пустым, соответствующий фильтр не применяется
- Результаты всегда сортируются по `seq` в порядке возрастания
- Реализовано в `JpaEventLogRepository` и `InMemoryEventLogRepository`

### REST Layer

- `EventLogResource` - JAX-RS ресурс
- `EventLogDTO` - DTO для сериализации EventLog

## Testing

Реализация протестирована в:
- `JpaEventLogRepositoryTest` (Hibernate persistence)
- `InMemoryEventLogRepositoryTest` (in-memory persistence)
