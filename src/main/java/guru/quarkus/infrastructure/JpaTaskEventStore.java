package guru.quarkus.infrastructure;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import guru.quarkus.domain.TaskConcurrencyException;
import guru.quarkus.domain.TaskEventStore;
import guru.quarkus.domain.events.DomainEvent;

@ApplicationScoped
public class JpaTaskEventStore implements TaskEventStore {

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public List<DomainEvent> fetch(String taskId) {
        requireNonNull(taskId, "'taskId' must not be null");
        return em.createQuery("""
                select e from TaskEventRecord e
                where e.aggregateId = :taskId
                order by e.version""", TaskEventRecord.class).setParameter("taskId", taskId).getResultStream()
                .map(this::deserialize).toList();
    }

    @Override
    public Map<String, List<DomainEvent>> fetchAll() {
        Map<String, List<DomainEvent>> eventsByTask = new LinkedHashMap<>();
        em.createQuery("select e from TaskEventRecord e order by e.id", TaskEventRecord.class).getResultStream()
                .forEach(record -> eventsByTask.computeIfAbsent(record.getAggregateId(), k -> new ArrayList<>())
                        .add(deserialize(record)));
        return eventsByTask;
    }

    @Override
    @Transactional
    public void append(String taskId, List<DomainEvent> domainEvents, long expectedVersion) {
        requireNonNull(taskId, "'taskId' must not be null");
        requireNonNull(domainEvents, "'domainEvents' must not be null");
        if (domainEvents.isEmpty()) {
            return;
        }

        long currentVersion = em.createQuery("""
                select coalesce(max(e.version), 0) from TaskEventRecord e
                where e.aggregateId = :taskId""", Long.class).setParameter("taskId", taskId).getSingleResult();
        if (currentVersion != expectedVersion) {
            throw new TaskConcurrencyException(taskId, expectedVersion, currentVersion);
        }

        var now = Instant.now();
        long version = expectedVersion;
        for (var domainEvent : domainEvents) {
            em.persist(new TaskEventRecord(taskId, ++version, domainEvent.getClass().getName(), serialize(domainEvent),
                    now));
        }

        try {
            // the unique (aggregate_id, version) constraint catches writers that raced past the check above
            em.flush();
        } catch (PersistenceException e) {
            throw new TaskConcurrencyException(taskId, expectedVersion, e);
        }
    }

    private String serialize(DomainEvent domainEvent) {
        try {
            return objectMapper.writer().without(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                    .writeValueAsString(domainEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize " + domainEvent.getClass().getName(), e);
        }
    }

    private DomainEvent deserialize(TaskEventRecord record) {
        try {
            var eventClass = Class.forName(record.getEventType()).asSubclass(DomainEvent.class);
            return objectMapper.readValue(record.getPayload(), eventClass);
        } catch (ClassNotFoundException | ClassCastException | JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize event type " + record.getEventType(), e);
        }
    }
}
