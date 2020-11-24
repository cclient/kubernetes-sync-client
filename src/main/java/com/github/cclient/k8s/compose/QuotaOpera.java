package com.github.cclient.k8s.compose;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ResourceQuota;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author cclient
 */
@Slf4j
public class QuotaOpera {

    static int memG = 1024 * 1024 * 1024;
    private final CoreV1Api api;

    public QuotaOpera() {
        api = Client.buildCoreApi();
    }

    public V1ResourceQuota getQuota(String namespace, String name) throws ApiException {
        V1ResourceQuota v1ResourceQuota = api.readNamespacedResourceQuota(name, namespace, null, false, false);
        log.info(v1ResourceQuota.getStatus().getUsed().toString());
        return v1ResourceQuota;
    }

    public V1ResourceQuota getQuota(String namespace) throws ApiException {
        return getQuota(namespace, "quota");
    }

    public List<Integer> getQuotaNum(String namespace) throws ApiException {
        return getQuotaNum(namespace, "quota");
    }

    public List<Integer> getQuotaNum(String namespace, String name) throws ApiException {
        V1ResourceQuota v1ResourceQuota = getQuota(namespace, name);
        ArrayList<Integer> quotaNums = new ArrayList<>(128);
        var used = v1ResourceQuota.getStatus().getUsed();
        quotaNums.add(used.get("requests.cpu").getNumber().intValue());
        quotaNums.add(used.get("requests.memory").getNumber().divide(new BigDecimal(memG)).intValue());
        quotaNums.add(used.get("requests.nvidia.com/gpu").getNumber().intValue());
        var hard = v1ResourceQuota.getStatus().getHard();
        quotaNums.add(hard.get("requests.cpu").getNumber().intValue());
        quotaNums.add(hard.get("requests.memory").getNumber().divide(new BigDecimal(memG)).intValue());
        quotaNums.add(hard.get("requests.nvidia.com/gpu").getNumber().intValue());
        return quotaNums;
    }

    /**
     * @param namespace
     * @return
     */
    public List<Integer> getQuotaNumSplit(String namespace) throws ApiException {
        val v1PodList = api.listNamespacedPod(namespace, null, false, null, null, null, null, null, 120, false);
        int cpuSumPod = 0;
        int memSumPod = 0;
        int gpuSumPod = 0;
        int cpuSumStatefulSet = 0;
        int memSumStatefulSet = 0;
        int gpuSumStatefulSet = 0;
        for (V1Pod pod : v1PodList.getItems()) {
            int cpuSum = 0;
            int memSum = 0;
            int gpuSum = 0;
            for (V1Container container : pod.getSpec().getContainers()) {
                val stringQuantityMap = container.getResources().getRequests();
                val cpu = stringQuantityMap.get("cpu").getNumber().intValue();
                val mem = stringQuantityMap.get("memory").getNumber().divide(new BigDecimal(memG)).intValue();
                val gpu = stringQuantityMap.get("nvidia.com/gpu").getNumber().intValue();
                cpuSum = cpuSum + cpu;
                memSum = memSum + mem;
                gpuSum = gpuSum + gpu;
            }
            String podName = pod.getMetadata().getName();
            //训练资源
            if (podName.startsWith("pod")) {
                cpuSumPod = cpuSumPod + cpuSum;
                memSumPod = memSumPod + memSum;
                gpuSumPod = gpuSumPod + gpuSum;
            }
            if (podName.startsWith("statefulset")) {
                cpuSumStatefulSet = cpuSumStatefulSet + cpuSum;
                memSumStatefulSet = memSumStatefulSet + memSum;
                gpuSumStatefulSet = gpuSumStatefulSet + gpuSum;
            }
        }
        return Arrays.asList(cpuSumPod, memSumPod, gpuSumPod, cpuSumStatefulSet, memSumStatefulSet, gpuSumStatefulSet);
    }
}
