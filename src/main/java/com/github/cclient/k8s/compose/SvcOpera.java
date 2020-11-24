package com.github.cclient.k8s.compose;


import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.val;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class SvcOpera {
    static Integer DEFAULT_PORT = 80;
    private final CoreV1Api api;

    public SvcOpera() {
        api = Client.buildCoreApi();
    }

    public V1Service build(String namespace, String name, Integer port) {
        if (port == null || port.intValue() == 0) {
            port = DEFAULT_PORT;
        }
        V1ServiceBuilder serviceBuilder = new V1ServiceBuilder();
        V1ServicePortBuilder portBuilder = new V1ServicePortBuilder();
        V1ServicePort servicePort = portBuilder.withPort(port).withTargetPort(new IntOrString(port)).withProtocol("TCP").build();
        V1Service v1Service = serviceBuilder
                .withNewApiVersion("v1")
                .withKind("Service")
                .withNewMetadata().withName(name).withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withType("NodePort").withPorts(servicePort).withSelector(new HashMap<>(4) {{
                    put(AbstractCompose.DEFAULT_LABEL_KEY, name);
                }})
                .endSpec().build();
        return v1Service;
    }

    public boolean exist(String namespace, String name) throws ApiException {
        V1ServiceList serviceList = api.listNamespacedService(namespace, null, false, null, "metadata.name=" + name, null, null, null, 60, false);
        return serviceList.getItems().size() > 0;
    }

    public Boolean deploy(V1Service svc) throws ApiException, InterruptedException {
        if (exist(svc.getMetadata().getNamespace(), svc.getMetadata().getName())) {
            return false;
        }
        svc = api.createNamespacedService(svc.getMetadata().getNamespace(), svc, null, null, null);
        while (true) {
            boolean deleted = exist(svc.getMetadata().getNamespace(), svc.getMetadata().getName());
            if (deleted) {
                return true;
            } else {
                TimeUnit.SECONDS.sleep(2);
                continue;
            }
        }
    }

    public Integer deployAndGetNodePort(V1Service svc) throws ApiException, InterruptedException {
        deploy(svc);
        svc = api.readNamespacedService(svc.getMetadata().getName(), svc.getMetadata().getNamespace(), null, false, false);
        return svc.getSpec().getPorts().get(0).getNodePort();
    }

    public Integer deployAndGetNodePort(String namespace, String name, Integer port) throws ApiException, InterruptedException {
        val svc = build(namespace, name, port);
        return deployAndGetNodePort(svc);
    }

    public Boolean delete(String namespace, String name) throws ApiException, InterruptedException {
        if (!exist(namespace, name)) {
            return true;
        }
        api.deleteNamespacedService(name, namespace, null, null, null, false, null, null);
        while (true) {
            boolean deleted = exist(namespace, name);
            if (deleted) {
                TimeUnit.SECONDS.sleep(2);
                continue;
            } else {
                return true;
            }
        }
    }
}
