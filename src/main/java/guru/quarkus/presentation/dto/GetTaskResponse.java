package guru.quarkus.presentation.dto;

import guru.quarkus.domain.TaskStatuses;

public record GetTaskResponse(String id, long version, TaskStatuses status, String description) {
}
