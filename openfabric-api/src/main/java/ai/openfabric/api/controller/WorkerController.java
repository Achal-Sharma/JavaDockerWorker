package ai.openfabric.api.controller;

import ai.openfabric.api.repository.WorkerRepository;
import ai.openfabric.api.model.Worker;
import com.github.dockerjava.api.*;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.*;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("${node.api.path}/worker")
public class WorkerController
{
    private DockerClientConfig config;
    private DockerClient dockerClient;
    private DockerHttpClient httpClient;
    private WorkerRepository workerRepository;
    private Worker worker;
    private static final Logger logger = LoggerFactory.getLogger(WorkerController.class);

    public WorkerController(WorkerRepository repository)
    {
        this.config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        this.httpClient = createDockerHttpClient(config);
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
        this.workerRepository = repository;
    }

    private DockerHttpClient createDockerHttpClient(DockerClientConfig config)
    {
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
    }

    // 1. List workers (paginated)
    @GetMapping(path = "/listWorkers")
    public Page<Worker> listWorkers(@RequestParam int page,
                                    @RequestParam int size,
                                    @RequestParam(defaultValue = "id,asc") String[] sort)
    {
        String sortBy = sort[0];
        String sortOrder = sort.length > 1 ? sort[1] : "asc";
        Sort.Direction direction = Sort.Direction.fromString(sortOrder);
        Sort sortable = Sort.by(direction, sortBy);

        Pageable pageRequest = PageRequest.of(page, size, sortable);
        return (Page<Worker>) workerRepository.findAll(pageRequest);
    }

    @PostMapping("/startWorker/{containerName}")
    public ResponseEntity<String> startDocker(@PathVariable String containerName)
    {
        boolean isActive;
        worker = workerRepository.findByName(containerName);

        try
        {
            ListContainersCmd listContainersCmd = dockerClient.listContainersCmd().withShowAll(true).withNameFilter(Collections.singletonList(containerName));
            List<Container> containers = listContainersCmd.exec();

            // if container doesn't exist in database
            if (worker == null)
            {
                // if actual container exists
                if (containers.size() == 1)
                {
                    String containerId = containers.get(0).getId();
                    InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
                    isActive = Boolean.TRUE.equals(inspectContainerResponse.getState().getRunning());

                    worker = new Worker();
                    worker.setName(containerName);
                    worker.setPort(getPortNumber(containerName));
                    workerRepository.save(worker);

                    if (!isActive)
                    {
                        dockerClient.startContainerCmd(containerName).exec();
                    }
                    worker.setStatus("active");
                    workerRepository.save(worker);
                    return ResponseEntity.ok(worker.getName() + " container is " + (isActive ? "already " : "") + "active...");
                }
                // if actual container doesn't exists
                else
                {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Container doesn't exist...");
                }
            }
            // if container exists in database
            else
            {
                // if actual container exists
                if (containers.size() == 1)
                {
                    String containerId = containers.get(0).getId();
                    InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
                    isActive = Boolean.TRUE.equals(inspectContainerResponse.getState().getRunning());

                    if (!isActive)
                    {
                        dockerClient.startContainerCmd(containerName).exec();
                    }
                    worker.setStatus("active");
                    workerRepository.save(worker);
                    return ResponseEntity.ok(worker.getName() + " container is " + (isActive ? "already " : "") + "active...");
                }
                // if actual container doesn't exist
                else
                {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Container doesn't exist...");
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Encountered an error while starting the container", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Refer to stack trace..." + e.getMessage());
        }
    }

    @PostMapping("/stopWorker/{containerName}")
    public ResponseEntity<String> stopDocker(@PathVariable String containerName)
    {
        boolean isInactive;
        worker = workerRepository.findByName(containerName);

        try
        {
            ListContainersCmd listContainersCmd = dockerClient.listContainersCmd().withShowAll(true).withNameFilter(Collections.singletonList(containerName));
            List<Container> containers = listContainersCmd.exec();

            // if container doesn't exist in database
            if (worker == null)
            {
                // if actual container exists
                if (containers.size() == 1)
                {
                    String containerId = containers.get(0).getId();
                    InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
                    isInactive = Boolean.FALSE.equals(inspectContainerResponse.getState().getRunning());

                    worker = new Worker();
                    worker.setName(containerName);
                    worker.setPort(getPortNumber(containerName));
                    workerRepository.save(worker);

                    if (!isInactive)
                    {
                        dockerClient.stopContainerCmd(containerName).exec();
                    }
                    worker.setStatus("inactive");
                    workerRepository.save(worker);
                    return ResponseEntity.ok(worker.getName() + " container is " + (isInactive ? "already " : "") + "inactive...");
                }
                // if actual container doesn't exists
                else
                {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Container doesn't exist...");
                }
            }
            // if container exists in database
            else
            {
                // if actual container exists
                if (containers.size() == 1)
                {
                    String containerId = containers.get(0).getId();
                    InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
                    isInactive = Boolean.FALSE.equals(inspectContainerResponse.getState().getRunning());

                    if (!isInactive)
                    {
                        dockerClient.stopContainerCmd(containerName).exec();
                    }
                    worker.setStatus("inactive");
                    workerRepository.save(worker);
                    return ResponseEntity.ok(worker.getName() + " container is " + (isInactive ? "already " : "") + "inactive...");
                }
                // if actual container doesn't exist
                else
                {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Container doesn't exist...");
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Encountered an error while stopping the container", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Refer to stack trace..." + e.getMessage());
        }
    }

    private String getPortNumber(String containerName)
    {
        return Objects.requireNonNull(dockerClient
                .listContainersCmd().withNameFilter(Collections.singleton(containerName)).exec()
                .stream()
                .findFirst()
                .map(container -> container.getPorts()[0].getPublicPort())
                .orElse(null)).toString();
    }
}
