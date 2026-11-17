package guru.quarkus.domain;

import java.util.List;
import java.util.Map;

import guru.quarkus.domain.events.DomainEvent;

public interface TaskEventStore {

    List<DomainEvent> fetch(String taskId);

    Map<String, List<DomainEvent>> fetchAll();

    void append(String taskId, List<DomainEvent> domainEvents, long expectedVersion);
}
