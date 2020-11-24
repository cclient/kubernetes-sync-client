package com.github.cclient.k8s.compose;


import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Yaml;
import lombok.val;

import java.util.concurrent.TimeUnit;

public class IngressOpera {
    static Integer DEFAULT_PORT = 80;
    private final ExtensionsV1beta1Api api;

    public IngressOpera() {
        api = Client.buildExtensionsV1beta1Api();
    }

    public ExtensionsV1beta1Ingress build(String namespace, String name, String host, String path, Integer port) {
        ExtensionsV1beta1IngressBuilder builder = new ExtensionsV1beta1IngressBuilder();
        ExtensionsV1beta1HTTPIngressPathBuilder ipb = new ExtensionsV1beta1HTTPIngressPathBuilder();
        ExtensionsV1beta1HTTPIngressRuleValueBuilder irvb = new ExtensionsV1beta1HTTPIngressRuleValueBuilder();
        ExtensionsV1beta1HTTPIngressPath ip = ipb.withNewBackend().withNewServiceName(name).withNewServicePort(port).endBackend().withNewPath(path).build();
        ExtensionsV1beta1Ingress ingress = builder.withNewMetadata()
                .withName(name)
                .withNamespace(namespace).endMetadata().withNewSpec()
                .addNewRule().withHost(host)
                .withHttp(irvb.withPaths(ip).build()).endRule()
                .endSpec().build();
        System.out.println((Yaml.dump(ingress)));
        return ingress;
    }

    public boolean exist(String namespace, String name) throws ApiException {
        ExtensionsV1beta1IngressList serviceList = api.listNamespacedIngress(namespace, null, false, null, "metadata.name=" + name, null, null, null, 60, false);
        return serviceList.getItems().size() > 0;
    }

    public Boolean deploy(ExtensionsV1beta1Ingress svc) throws ApiException, InterruptedException {
        if (exist(svc.getMetadata().getNamespace(), svc.getMetadata().getName())) {
            return false;
        }
        svc = api.createNamespacedIngress(svc.getMetadata().getNamespace(), svc, null, null, null);
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

    public Boolean deploy(String namespace, String name, String host, String path, Integer port) throws ApiException, InterruptedException {
        val svc = build(namespace, name, host, path, port);
        return deploy(svc);
    }

    public Boolean delete(String namespace, String name) throws ApiException, InterruptedException {
        if (!exist(namespace, name)) {
            return true;
        }
        api.deleteNamespacedIngress(name, namespace, null, null, null, false, null, null);
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
