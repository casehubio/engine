package io.casehub.engine.internal.worker;

import io.casehub.api.model.ProvisionContext;
import io.casehub.api.model.Worker;
import io.casehub.api.spi.ProvisioningException;
import io.casehub.api.spi.WorkerProvisioner;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;

/**
 * Default no-op WorkerProvisioner that throws on every provision() call.
 * Signals misconfiguration — replace with a real implementation (e.g. Claudony's
 * ClaudonyWorkerProvisioner) before provisioning is needed.
 */
@ApplicationScoped
public class NoOpWorkerProvisioner implements WorkerProvisioner {

    @Override
    public Worker provision(Set<String> capabilities, ProvisionContext context) {
        throw new ProvisioningException(
                "No WorkerProvisioner configured — add an @ApplicationScoped WorkerProvisioner implementation");
    }

    @Override
    public void terminate(String workerId) {
        // intentional no-op — nothing to terminate
    }

    @Override
    public Set<String> getCapabilities() {
        return Set.of();
    }
}
