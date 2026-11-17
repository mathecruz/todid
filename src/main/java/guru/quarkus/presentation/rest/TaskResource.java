package guru.quarkus.presentation.rest;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import guru.quarkus.domain.Task;
import guru.quarkus.domain.TaskRepository;
import guru.quarkus.domain.TaskStatuses;
import guru.quarkus.domain.events.DomainEvent;
import guru.quarkus.domain.events.TaskDeleted;
import guru.quarkus.domain.events.TaskDone;
import guru.quarkus.domain.events.TaskInProgress;
import guru.quarkus.domain.events.TaskInToDo;
import guru.quarkus.presentation.dto.ChangeTaskStatusRequest;
import guru.quarkus.presentation.dto.CreateTaskRequest;
import guru.quarkus.presentation.dto.GetTaskResponse;

@Path("/api/v1/tasks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {

    @Inject
    TaskRepository taskRepository;

    @POST
    public Response createTask(CreateTaskRequest request) {

        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Task task;
        try {
            task = Task.create(request.description());
        } catch (NullPointerException | IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        taskRepository.save(task);

        return Response.created(URI.create("/api/v1/tasks/" + task.getId())).build();
    }

    @GET
    public List<GetTaskResponse> findAll() {
        return taskRepository.findAll().stream().filter(task -> task.getStatus() != TaskStatuses.DELETED).map(
                task -> new GetTaskResponse(task.getId(), task.getVersion(), task.getStatus(), task.getDescription()))
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") String id) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response
                .ok(new GetTaskResponse(task.getId(), task.getVersion(), task.getStatus(), task.getDescription()))
                .build();
    }

    @PATCH
    @Path("/{id}")
    public Response changeStatus(@PathParam("id") String id, ChangeTaskStatusRequest request) {

        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        DomainEvent event = switch (request.status()) {
            case IN_PROGRESS -> new TaskInProgress();
            case DELETED -> new TaskDeleted();
            case DONE -> new TaskDone();
            case TODO -> new TaskInToDo();
        };

        task.appendEvent(event);
        taskRepository.save(task);

        return Response.status(Response.Status.OK).build();
    }
}
