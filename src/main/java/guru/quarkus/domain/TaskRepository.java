package guru.quarkus.domain;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import guru.quarkus.domain.events.DomainEvent;

@ApplicationScoped
public class TaskRepository {

    @Inject
    TaskEventStore taskEventStore;

    public Optional<Task> findById(String uuid) {
        List<DomainEvent> domainEvents = taskEventStore.fetch(uuid);
        if (domainEvents.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Task(domainEvents));
    }

    public List<Task> findAll() {
        return taskEventStore.fetchAll().values().stream().map(Task::new).toList();
    }

    public void save(Task task) {
        List<DomainEvent> newEvents = task.getDomainEvents();
        long persistedVersion = task.getVersion() + 1 - newEvents.size();
        taskEventStore.append(task.getId(), newEvents, persistedVersion);
    }
}
