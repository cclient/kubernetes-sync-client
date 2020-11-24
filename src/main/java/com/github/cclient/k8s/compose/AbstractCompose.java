package com.github.cclient.k8s.compose;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractCompose<T, R, L> implements ICompose<T, R, L> {
    public static String DEFAULT_NAMESPACE = "default";
    public static String DEFAULT_IMAGE_PULLSECRET = "docker_secret";
    public static String DEFAULT_RESTART_POLICY = "Always";
    public static String DEFAULT_LABEL_KEY = "cclient-deploy";
    private final QuotaOpera quotaOpera;
    protected CoreV1Api coreV1Api;
    protected AppsV1Api appsV1Api;

    public AbstractCompose() {
        coreV1Api = Client.buildCoreApi();
        appsV1Api = Client.buildAppsV1Api();
        quotaOpera = new QuotaOpera();
    }

    public V1Container buildContainer(String image, List<String> cmds, Integer request_cpu, Integer request_mem, Integer request_gpu) {
        V1ContainerBuilder containerBuilder = new V1ContainerBuilder();
        V1Container container = containerBuilder.withImage(image)
                .addAllToCommand(cmds)
                .withName("base")
                .withImagePullPolicy("Always")
                .withNewResources()
                .addToLimits("memory", new Quantity(request_mem + "Gi"))
                .addToLimits("cpu", new Quantity(String.valueOf(request_cpu)))
                .addToLimits("nvidia.com/gpu", new Quantity(String.valueOf(request_gpu)))
                .endResources()
                .build();
        return container;
    }


    public V1ObjectMeta buildMetaData(String namespace, String name, Map<String, String> labels) {
        labels.put(DEFAULT_LABEL_KEY, name);
        V1ObjectMetaBuilder builder = new V1ObjectMetaBuilder();
        return builder.withNamespace(namespace)
                .withName(name)
                .withLabels(labels).build();
    }

    public boolean checkQuotaSatisfy(String namespace, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) throws ApiException {
        if (replicas == null || replicas.intValue() == 0) {
            replicas = request_gpu;
        }
        List<Integer> quotaNums = quotaOpera.getQuotaNum(namespace);
        log.debug("current cpu:" + quotaNums.get(0) + "hard cpu:" + quotaNums.get(3) + " to cpu:" + (quotaNums.get(0) + request_cpu * replicas));
        if (quotaNums.get(0) + request_cpu * replicas > quotaNums.get(3)) {
            return false;
        }
        log.debug("current mem:" + quotaNums.get(1) + "hard mem:" + quotaNums.get(4) + " to mem:" + (quotaNums.get(1) + request_mem * replicas));
        if (quotaNums.get(1) + request_mem * replicas > quotaNums.get(4)) {
            return false;
        }
        log.debug("current gpu:" + quotaNums.get(2) + "hard gpu:" + quotaNums.get(5) + " to gpu:" + (quotaNums.get(2) + request_gpu * replicas));
        return quotaNums.get(2) + request_gpu * replicas <= quotaNums.get(5);
    }

    public List<V1Toleration> tolerationFormat(List<Map<String, String>> tolerations) {
        var toleratioList = new ArrayList<V1Toleration>();
        if (tolerations == null) {
            return toleratioList;
        }
        for (Map<String, String> m : tolerations) {
            var tb = new V1TolerationBuilder();
            if (m.containsKey("key")) {
                tb.withKey(m.get("key"));
            }
            if (m.containsKey("effect")) {
                tb.withEffect(m.get("effect"));
            }
            toleratioList.add(tb.build());
        }
        return toleratioList;
    }

    @Override
    public String buildYaml(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) {
        var compose = build(namespace, name, image, envs, cmds, args, labels, volumeMounts, volumes, tolerations, request_cpu, request_mem, request_gpu, replicas);
        return toYaml(compose);
    }

    public String toYaml(T compose) {
        return Yaml.dump(compose);
    }

    @Override
    public Boolean checkRunningFinished(String namespace, String name) throws Exception {
        return false;
    }

    @Override
    public Boolean checkRunningFinished(T compose) throws Exception {
        return false;
    }

    @Override
    public Boolean deleteTillSuccessed(String namespace, String name) throws Exception {
        //删除
        boolean deleted = delete(namespace, name);
        log.debug("target-this deleted", deleted);
        //如果有子检查子都被删除
        while (true) {
            deleted = checkDeletedSuccess(namespace, name);
            log.debug("target-sub deleted");
            if (deleted) {
                return true;
            } else {
                TimeUnit.SECONDS.sleep(20);
            }
        }
    }

    @Override
    public T runAsync(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) throws Exception {
        boolean isSatisfy = checkQuotaSatisfy(namespace, request_cpu, request_mem, request_gpu, replicas);
        if (!isSatisfy) {
            throw new Exception("K8s资源不足，不可提交");
        }
        var compose = build(namespace, name, image, envs, cmds, args, labels, volumeMounts, volumes, tolerations, request_cpu, request_mem, request_gpu, replicas);
        compose = runAsync(compose);
        return compose;
    }

    @Override
    public Boolean runTillRunning(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) throws Exception {
        boolean isSatisfy = checkQuotaSatisfy(namespace, request_cpu, request_mem, request_gpu, replicas);
        if (!isSatisfy) {
            log.info("K8s资源不足，不可提交");
            return false;
        }
        var compose = build(namespace, name, image, envs, cmds, args, labels, volumeMounts, volumes, tolerations, request_cpu, request_mem, request_gpu, replicas);
        return runTillRunning(compose);
    }

    @Override
    public Boolean runTillRunning(T pod) throws Exception {
        //检查是否已有在运行中
        if (exist(pod)) {
            log.debug("pod/set 已存在");
            log.debug(pod.toString());
            throw new Exception("已在运行中,不可重复提交");
        }
        log.info("任务不存在，启动任务");
        pod = runAsync(pod);
        log.info("任务提交完成-需检查运行状态");
        //如果有子检查子都启动
        while (true) {
            log.info("检查运行状态");
            boolean deployed = checkRunningSuccess(pod);
            if (deployed) {
                log.info("确认任务运行中");
                return true;
            } else {
                TimeUnit.SECONDS.sleep(30);
            }
        }
    }

}
