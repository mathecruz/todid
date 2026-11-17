package guru.quarkus.presentation.dto;

import guru.quarkus.domain.TaskStatuses;

public record ChangeTaskStatusRequest(TaskStatuses status) {
}
