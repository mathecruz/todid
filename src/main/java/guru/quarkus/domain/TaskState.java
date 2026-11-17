package guru.quarkus.domain;

import guru.quarkus.domain.events.TaskCreated;
import guru.quarkus.domain.events.TaskDeleted;
import guru.quarkus.domain.events.TaskDone;
import guru.quarkus.domain.events.TaskInProgress;
import guru.quarkus.domain.events.TaskInToDo;

public class TaskState {

    private String id;
    private Long version;
    private String description;
    private TaskStatuses status;

    public void apply(TaskCreated taskCreated) {
        this.id = taskCreated.getId();
        this.description = taskCreated.getDescription();
        this.version = 0L;
        this.status = taskCreated.status();
    }

    public void apply(TaskDone taskDone) {
        this.version = this.version + 1;
        this.status = taskDone.status();
    }

    public void apply(TaskInProgress taskInProgress) {
        this.version = this.version + 1;
        this.status = taskInProgress.status();
    }

    public void apply(TaskDeleted taskDeleted) {
        this.version = this.version + 1;
        this.status = taskDeleted.status();
    }

    public void apply(TaskInToDo taskInToDo) {
        this.version = this.version + 1;
        this.status = taskInToDo.status();
    }

    public TaskStatuses getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
