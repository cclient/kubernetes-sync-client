package com.github.cclient.k8s.compose;


import com.github.cclient.k8s.enums.PodStatus;
import com.github.cclient.k8s.enums.State;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;


@Slf4j
public class PodOpera extends AbstractCompose<V1Pod, V1Pod, V1Pod> implements ICompose<V1Pod, V1Pod, V1Pod> {
    public static String metaNamePrefix = "pod";
    private final CoreV1Api api;

    public PodOpera() {
        this.api = super.coreV1Api;
    }

    @Override
    public V1Pod build(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) {
        var podbuilder = new V1PodBuilder();
        var tolerationList = tolerationFormat(tolerations);
        V1Container container = buildContainer(image, cmds, request_cpu, request_mem, request_gpu);
        V1ObjectMeta meta = buildMetaData(namespace, metaNamePrefix + "-" + name, labels);
        var pod = podbuilder
                //初始化meta
                .withMetadata(meta)
                //构造spec
                .editOrNewSpec()
                //构造container
                .addToContainers(container)
//                .addNewImagePullSecret().withName(DEFAULT_IMAGE_PULLSECRET).endImagePullSecret()
                .addAllToTolerations(tolerationList)
                .withRestartPolicy("Never")
                .endSpec()
                .build();
        return pod;
    }

    @Override
    public V1Pod get(String namespace, String name) throws Exception {
        V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, "metadata.name=" + name, null, null, null, 120, false);
        if (v1PodList.getItems().size() > 0) {
            return v1PodList.getItems().get(0);
        }
        return null;
    }

    @Override
    public Boolean exist(String namespace, String name) throws ApiException {
        V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, "metadata.name=" + name, null, null, null, 120, false);
        return v1PodList.getItems().size() > 0;
    }

    @Override
    public Boolean exist(V1Pod compose) throws Exception {
        return exist(compose.getMetadata().getNamespace(), compose.getMetadata().getName());
    }

    @Override
    public V1Pod runAsync(V1Pod pod) throws Exception {
        try {
            var runningPod = api.createNamespacedPod(pod.getMetadata().getNamespace(), pod, null, null, null);
            return runningPod;
        } catch (ApiException e) {
            e.printStackTrace();
            throw new Exception("Exception when attempting to create Namespaced Pod:" + pod.toString());
        }
    }

    public State process_status(String job_id, String status) {
        status = status.toLowerCase();
        log.debug("pod status");
        log.debug(status);
        if (status.equals(PodStatus.PENDING.getValue())) {
            return State.QUEUED;
        } else if (status.equals(PodStatus.FAILED.getValue())) {
            return State.FAILED;
        } else if (status.equals(PodStatus.SUCCEEDED.getValue())) {
            return State.SUCCESS;
        } else if (status.equals(PodStatus.RUNNING.getValue())) {
            return State.RUNNING;
        } else {
            return State.FAILED;
        }
    }

    public State _task_status(V1Pod event) {
        var status = process_status(event.getMetadata().getName(), event.getStatus().getPhase());
        return status;
    }


    public V1Pod read_pod(V1Pod pod) {
        try {
            var podinfo = api.readNamespacedPod(pod.getMetadata().getName(), pod.getMetadata().getNamespace(), null, null, null);
            log.debug("获取任务状态");
            return podinfo;
        } catch (ApiException apiException) {
            apiException.printStackTrace();
        }
        return pod;
    }


    public boolean pod_is_running(V1Pod pod) {
        var state = _task_status(read_pod(pod));
        return state != State.SUCCESS && state != State.FAILED;
    }


    public boolean pod_is_run(V1Pod pod) {
        var state = _task_status(read_pod(pod));
        return state != State.QUEUED;
    }

    /**
     * 判断pod是否结束
     * 0 运行中
     * 1 成功
     * 2 失败
     *
     * @param pod
     * @return
     * @throws Exception
     */
    public Integer pod_is_finish(V1Pod pod) throws Exception {
        return pod_is_finish(pod.getMetadata().getNamespace(), pod.getMetadata().getName());
    }

    /**
     * 判断pod是否结束
     * 0 运行中
     * 1 成功
     * 2 失败
     *
     * @return
     */
    public Integer pod_is_finish(String namespace, String name) {
        var state = _task_status(read_pod(namespace, name));
        log.info("pod ns: {} name: {} state: {}", namespace, name, state);
        if (state == State.SUCCESS) {
            return 1;
        }
        if (state == State.FAILED) {
            return 2;
        }
        return 0;
    }

    @SneakyThrows
    public V1Pod read_pod(String namespace, String name) {
        try {
            var podinfo = api.readNamespacedPod(name, namespace, null, null, null);
            return podinfo;
        } catch (ApiException apiException) {
            apiException.printStackTrace();
            throw new Exception("read_pod error ");
        }
    }


    @Override
    public Boolean checkRunningSuccess(String namespace, String name) throws Exception {
        V1Pod pod = get(namespace, name);
        return checkRunningSuccess(pod);
    }

    @Override
    public Boolean checkRunningSuccess(V1Pod pod) throws Exception {
        return pod_is_run(pod);
    }

    @Override
    public Boolean checkDeletedSuccess(String namespace, String name) throws Exception {
        return !exist(namespace, name);
    }

    @Override
    public Boolean checkDeletedSuccess(V1Pod compose) throws Exception {
        return !exist(compose);
    }

    public V1Pod composeDelete(String namespace, String name) throws ApiException {
        V1DeleteOptions v1DeleteOptions = new V1DeleteOptions();
        V1Pod v1Status = coreV1Api.deleteNamespacedPod(name, namespace, null, null, 60, true, null, v1DeleteOptions);
        System.out.println(v1Status);
        return v1Status;
    }

    @Override
    public Boolean delete(String namespace, String name) throws Exception {
        boolean isExist = exist(namespace, name);
        if (!isExist) {
            throw new Exception("K8s不存在相关服务，无法删除");
        }
        try {
            V1Pod v1Status = composeDelete(namespace, name);
            System.out.println(v1Status);
        } catch (Exception e) {
            System.out.println(e);
            if (e.getCause() instanceof IllegalStateException) {
                IllegalStateException ise = (IllegalStateException) e.getCause();
                if (ise.getMessage() != null && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
                    log.info("Catching exception because of issue https://github.com/kubernetes/kubernetes/issues/65121");
                } else {
                    //...throw error  or log
                }
            } else {
                //...throw error or log
            }
        }
        isExist = exist(namespace, name);
        return !isExist;
    }

    public String read_pod_logs(String namespace, String name) throws ApiException {
        return api.readNamespacedPodLog(name, namespace, null, null, null, null, null, null, 100, null,null);
    }

    public String read_pod_logs(V1Pod pod) throws ApiException {
        return read_pod_logs(pod.getMetadata().getNamespace(), pod.getMetadata().getName());
    }

}
