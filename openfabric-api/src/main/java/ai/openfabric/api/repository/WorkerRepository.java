package ai.openfabric.api.repository;

import ai.openfabric.api.model.Worker;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface WorkerRepository extends CrudRepository<Worker, String>
{
    Object findAll(Pageable pageRequest);
}
