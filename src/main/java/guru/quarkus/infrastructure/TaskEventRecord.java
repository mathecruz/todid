package guru.quarkus.infrastructure;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "task_events", uniqueConstraints = @UniqueConstraint(columnNames = { "aggregate_id", "version" }))
public class TaskEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(nullable = false)
    private long version;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;

    protected TaskEventRecord() {
    }

    public TaskEventRecord(String aggregateId, long version, String eventType, String payload, Instant occurredOn) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredOn = occurredOn;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public long getVersion() {
        return version;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }
}
