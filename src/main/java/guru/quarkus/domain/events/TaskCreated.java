package guru.quarkus.domain.events;

import java.util.UUID;

import guru.quarkus.domain.TaskState;
import guru.quarkus.domain.TaskStatuses;

public class TaskCreated extends DomainEvent {

    private final String id;
    private final String description;

    public TaskCreated(String description) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
    }

    @Override
    public void applyTo(TaskState state) {
        state.apply(this);
    }

    @Override
    public TaskStatuses status() {
        return TaskStatuses.TODO;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
