package guru.quarkus.domain.events;

import guru.quarkus.domain.TaskState;
import guru.quarkus.domain.TaskStatuses;

public class TaskInToDo extends DomainEvent {

    @Override
    public void applyTo(TaskState state) {
        state.apply(this);
    }

    @Override
    public TaskStatuses status() {
        return TaskStatuses.TODO;
    }
}
