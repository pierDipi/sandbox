package com.redhat.service.bridge.shard.operator.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.networking.NetworkingTestUtils;

import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationScoped
public class KubernetesResourcePatcher {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NetworkingTestUtils networkingTestUtils;

    public void cleanUp() {
        kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().delete();
        kubernetesClient.resources(BridgeExecutor.class).inAnyNamespace().delete();
    }

    public void patchReadyDeploymentOrFail(String name, String namespace) {
        // Retrieve the deployment
        Deployment deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();

        // Fail if it has not been deployed yet
        assertThat(deployment).isNotNull();

        // Patch the deployment - This is what k8s would do when the resource is deployed and is ready.
        deployment.setStatus(new DeploymentStatusBuilder().withAvailableReplicas(1).withReplicas(1).build());
        kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .replace(deployment);
    }

    public void patchReadyServiceOrFail(String name, String namespace) {
        // Retrieve the service
        Service service = kubernetesClient.services()
                .inNamespace(namespace)
                .withName(name)
                .get();

        // Fail if it has not been deployed yet
        assertThat(service).isNotNull();

        // Patch the service - This is what k8s would do when the resource is deployed and is ready.
        service.setStatus(new ServiceStatusBuilder().withLoadBalancer(new LoadBalancerStatus()).build());
        kubernetesClient.services()
                .inNamespace(namespace)
                .withName(name)
                .replace(service);
    }

    public void patchReadyNetworkResourceOrFail(String name, String namespace) {
        // Retrieve the network resource
        Namespaced resource = networkingTestUtils.getNetworkResource(name, namespace);

        // Fail if it has not been deployed yet
        assertThat(resource).isNotNull();

        // Patch the network resource - This is what k8s would do when the resource is deployed and is ready.
        networkingTestUtils.patchNetworkResource(name, namespace);
    }
}
