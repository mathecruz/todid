package guru.quarkus.domain;

public class TaskConcurrencyException extends RuntimeException {

    public TaskConcurrencyException(String taskId, long expectedVersion, long actualVersion) {
        super("Task '%s' expected version %d but found %d".formatted(taskId, expectedVersion, actualVersion));
    }

    public TaskConcurrencyException(String taskId, long expectedVersion, Throwable cause) {
        super("Task '%s' was modified concurrently (expected version %d)".formatted(taskId, expectedVersion), cause);
    }
}
