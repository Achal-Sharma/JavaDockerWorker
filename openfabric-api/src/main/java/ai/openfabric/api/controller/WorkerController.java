package ai.openfabric.api.controller;

import ai.openfabric.api.repository.WorkerRepository;
import ai.openfabric.api.model.Worker;
import com.github.dockerjava.api.*;
import com.github.dockerjava.core.*;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.time.Duration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${node.api.path}/worker")
public class WorkerController
{
    private DockerClientConfig config;
    private DockerClient dockerClient;
    private DockerHttpClient httpClient;
    private WorkerRepository workerRepository;

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
}
