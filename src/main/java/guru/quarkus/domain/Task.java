package guru.quarkus.domain;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import guru.quarkus.domain.events.DomainEvent;
import guru.quarkus.domain.events.TaskCreated;

public class Task {

    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private final TaskState state = new TaskState();

    public Task(List<DomainEvent> domainEvents) {
        for (var domainEvent : requireNonNull(domainEvents, "'domainEvents' must not be null")) {
            // replaying persisted history
            requireNonNull(domainEvent, "'domainEvent' must not be null").applyTo(this.state);
        }
    }

    public static Task create(String description) {
        return new Task(description);
    }

    private Task(String description) {
        if (requireNonNull(description, "'description' must not be null").isBlank()) {
            throw new IllegalArgumentException("'description' must not be blank");
        }
        appendEvent(new TaskCreated(description));
    }

    public void appendEvent(DomainEvent domainEvent) {
        requireNonNull(domainEvent, "'domainEvent' must not be null").applyTo(this.state);
        this.domainEvents.add(domainEvent);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }

    public String getId() {
        return this.state.getId();
    }

    public Long getVersion() {
        return this.state.getVersion();
    }

    public String getDescription() {
        return this.state.getDescription();
    }

    public TaskStatuses getStatus() {
        return this.state.getStatus();
    }
}
