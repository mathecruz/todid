package guru.quarkus.domain.events;

import java.time.Instant;

import guru.quarkus.domain.TaskState;
import guru.quarkus.domain.TaskStatuses;

public abstract class DomainEvent {

    public abstract void applyTo(TaskState state);

    protected Instant timestamp;

    protected abstract TaskStatuses status();

    public DomainEvent() {
        this.timestamp = Instant.now();
    }
}
