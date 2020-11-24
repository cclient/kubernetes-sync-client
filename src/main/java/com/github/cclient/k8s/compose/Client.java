package com.github.cclient.k8s.compose;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Client {
    static private ApiClient client;

    static {
        InputStream inputStream = Client.class.getResourceAsStream("/kube.config.yaml");
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        KubeConfig kubeConfig = KubeConfig.loadKubeConfig(inputStreamReader);
        try {
            client = ClientBuilder.kubeconfig(kubeConfig).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Configuration.setDefaultApiClient(client);
    }

    public static CoreV1Api buildCoreApi() {
        CoreV1Api api = new CoreV1Api();
        return api;
    }

    public static AppsV1Api buildAppsV1Api() {
        AppsV1Api api = new AppsV1Api();
        return api;
    }

    public static ExtensionsV1beta1Api buildExtensionsV1beta1Api() {
        ExtensionsV1beta1Api api = new ExtensionsV1beta1Api();
        return api;
    }

}
