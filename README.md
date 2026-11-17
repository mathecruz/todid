# todid

An **event-sourced** to-do API built with [Quarkus](https://quarkus.io/), based on the concepts from Vlad Khononov's
book [*Learning Domain-Driven Design*](https://www.oreilly.com/library/view/learning-domain-driven-design/9781098100124/).

Instead of storing the current state of each task, the application stores **every change as a domain event**. A task's
state is always rebuilt by replaying its event history.

## Stack

- Java 21
- Quarkus 3 (Quarkus REST + Jackson, Hibernate ORM, SmallRye OpenAPI)
- PostgreSQL, with events stored as JSON

## How it works

### Aggregate and events

`Task` is the aggregate. Every state change becomes a `DomainEvent`:

| Event            | Resulting status |
|------------------|------------------|
| `TaskCreated`    | `TODO`           |
| `TaskInProgress` | `IN_PROGRESS`    |
| `TaskDone`       | `DONE`           |
| `TaskInToDo`     | `TODO`           |
| `TaskDeleted`    | `DELETED`        |

Each event is applied to a `TaskState`, which holds the id, description, status and version. To load a task, the
`TaskRepository` fetches its events from the event store and replays them in order.

```
POST/PATCH ─▶ TaskResource ─▶ Task.appendEvent(event) ─▶ TaskRepository.save
                                                                │
                                                                ▼
GET ◀── Task(events) ◀── TaskRepository.findById ◀── TaskEventStore (task_events)
```

A deleted task is not removed: it receives a `TaskDeleted` event and stops showing up in the listing, but its history is
kept.

### Event store

`JpaTaskEventStore` implements `TaskEventStore`. Events live in the append-only `task_events` table:

| Column         | Description                                |
|----------------|--------------------------------------------|
| `aggregate_id` | task id                                    |
| `version`      | position of the event in the task history  |
| `event_type`   | event class name                           |
| `payload`      | event serialized as JSON                   |
| `occurred_on`  | when the event was stored                  |

### Optimistic concurrency

When saving, the repository passes the version of the task it read. The event store compares it with the latest stored
version and throws `TaskConcurrencyException` if they differ. A unique constraint on `(aggregate_id, version)` also
rejects concurrent writes that pass that check at the same time.

### Project structure

```
src/main/java/guru/quarkus/
├── domain/            # Task aggregate, state, events, repository and event store port
│   └── events/
├── infrastructure/    # JPA/PostgreSQL event store
└── presentation/      # REST resource and DTOs
    ├── dto/
    └── rest/
```

## API

Base path: `/api/v1/tasks`

| Method  | Path    | Description                                | Body                          |
|---------|---------|--------------------------------------------|-------------------------------|
| `POST`  | `/`     | Creates a task (`201` with `Location`)     | `{"description": "..."}`      |
| `GET`   | `/`     | Lists tasks that have not been deleted     |                               |
| `GET`   | `/{id}` | Gets a task by id                          |                               |
| `PATCH` | `/{id}` | Changes the task status                    | `{"status": "IN_PROGRESS"}`   |

Accepted statuses: `TODO`, `IN_PROGRESS`, `DONE`, `DELETED`.

Example:

```shell
# create a task
curl -i -X POST localhost:8080/api/v1/tasks \
  -H 'Content-Type: application/json' \
  -d '{"description": "Read chapter 7"}'

# change its status (use the id from the Location header)
curl -X PATCH localhost:8080/api/v1/tasks/<id> \
  -H 'Content-Type: application/json' \
  -d '{"status": "DONE"}'

# list tasks
curl localhost:8080/api/v1/tasks
```

```json
[
  {
    "id": "3f1c...",
    "version": 1,
    "status": "DONE",
    "description": "Read chapter 7"
  }
]
```

In dev mode, the OpenAPI docs are available at <http://localhost:8080/q/swagger-ui>.

## Running locally

Requirements: Java 21 and Docker (or Podman).

```shell
./mvnw quarkus:dev
```

In dev mode, Quarkus Dev Services starts a PostgreSQL container and creates the `task_events` table automatically. The
Dev UI is at <http://localhost:8080/q/dev/>, where you can query the event table directly.

## Build

```shell
# JAR
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar

# native executable (built in a container, no local GraalVM needed)
./mvnw package -Dnative -Dquarkus.native.container-build=true
./target/todid-1.0.0-SNAPSHOT-runner
```

Outside dev mode, configure the database with `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME` and
`QUARKUS_DATASOURCE_PASSWORD`, and create the `task_events` table yourself: automatic schema generation is only enabled
in dev mode.

Dockerfiles for the JVM and native builds are in `src/main/docker/`.
