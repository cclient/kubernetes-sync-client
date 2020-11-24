package com.github.cclient.k8s.compose;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StatefulsetOpera extends AbstractCompose<V1StatefulSet, V1StatefulSet, V1StatefulSet> implements ICompose<V1StatefulSet, V1StatefulSet, V1StatefulSet> {

    public static String metaNamePrefix = "statefulset";
    AppsV1Api api;
    PodOpera podOpera;

    public StatefulsetOpera() {
        this.api = Client.buildAppsV1Api();
        this.podOpera = new PodOpera();
    }

    @Override
    public V1StatefulSet build(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) {
        var builder = new V1StatefulSetBuilder();
        var tolerationList = tolerationFormat(tolerations);
        if (replicas == 0) {
            replicas = request_gpu;
        }
        V1Container container = buildContainer(image, cmds, request_cpu, request_mem, request_gpu);
        V1ObjectMeta meta = buildMetaData(namespace, metaNamePrefix + "-" + name, labels);

        var statefulSet = builder
                //初始化meta
                .withMetadata(meta)
                //构造spec
                .editOrNewSpec().withReplicas(replicas).withServiceName(name)
                .withNewSelector().withMatchLabels(labels).endSelector()
                .withNewTemplate().withNewMetadata().withLabels(labels).endMetadata()
                .withNewSpec().withContainers()
                //构造container
                .addToContainers(container)
//                .addNewImagePullSecret().withName(AbstractCompose.DEFAULT_IMAGE_PULLSECRET).endImagePullSecret()
                .addAllToTolerations(tolerationList)
                .withRestartPolicy("Always")
                .endSpec()
                .endTemplate()
                .endSpec().build();
        return statefulSet;
    }

    @Override
    public V1StatefulSet get(String namespace, String name) throws Exception {
        V1StatefulSetList v1StatefulSetList = appsV1Api.listNamespacedStatefulSet(namespace, null, null, null, "metadata.name=" + name, null, null, null, 120, false);
        if (v1StatefulSetList.getItems().size() > 0) {
            return v1StatefulSetList.getItems().get(0);
        }
        return null;
    }


    public Boolean scale(String namespace, String name, Integer replicas) throws Exception {
        V1StatefulSet statefulSet = get(namespace, name);
        int oldReplicas = statefulSet.getSpec().getReplicas();
        if (oldReplicas == replicas) {
            log.debug("不需要变更副本数");
        } else {
            statefulSet.getSpec().setReplicas(replicas);
            statefulSet = api.replaceNamespacedStatefulSet(name, namespace, statefulSet, null, null, null);
            if (replicas > oldReplicas) {
                checkRunningSuccess(statefulSet);
            } else {
                while (true) {
                    V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, AbstractCompose.DEFAULT_LABEL_KEY + "=" + name, null, null, 120, false);
                    log.debug("current num" + v1PodList.getItems().size());
                    if (v1PodList.getItems().size() == replicas) {
                        return true;
                    } else {
                        TimeUnit.SECONDS.sleep(10);
                        continue;
                    }
                }
            }
        }
        return true;
    }


    /**
     * 检查缩/扩容类别
     *
     * @param namespace
     * @param name
     * @return
     * @throws Exception
     */
    public Integer getCurrentReplicas(String namespace, String name) throws Exception {
        V1StatefulSet statefulSet = get(namespace, name);
        int oldReplicas = statefulSet.getSpec().getReplicas();
        return oldReplicas;
    }

    public Boolean checkScaleSatisfy(String namespace, String name, Integer request_cpu, Integer request_mem, Integer request_gpu) throws Exception {
        return checkScaleSatisfy(namespace, name, request_cpu, request_mem, request_gpu, request_gpu);
    }

    public Boolean checkScaleSatisfy(String namespace, String name, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) throws Exception {
        if (replicas == null || replicas.intValue() == 0) {
            replicas = request_gpu;
        }
        V1StatefulSet statefulSet = get(namespace, name);
        int oldReplicas = statefulSet.getSpec().getReplicas();
        int needPod = replicas - oldReplicas;
        if (needPod > 0) {
            return checkQuotaSatisfy(namespace, request_cpu, request_mem, request_gpu, needPod);
        }
        return true;
    }

    @Override
    public Boolean exist(String namespace, String name) throws Exception {
        V1StatefulSetList v1DeploymentList = appsV1Api.listNamespacedStatefulSet(namespace, null, null, null, "metadata.name=" + name, null, null, null, 120, false);
        System.out.println(v1DeploymentList);
        log.debug("v1DeploymentList", v1DeploymentList);
        return v1DeploymentList.getItems().size() > 0;

    }

    @Override
    public Boolean exist(V1StatefulSet compose) throws Exception {
        return exist(compose.getMetadata().getNamespace(), compose.getMetadata().getName());
    }

    @Override
    public V1StatefulSet runAsync(V1StatefulSet pod) throws Exception {
        try {
            var runningPod = api.createNamespacedStatefulSet(pod.getMetadata().getNamespace(), pod, null, null, null);
            return runningPod;
        } catch (ApiException e) {
            e.printStackTrace();
            throw new Exception("Exception when attempting to create Namespaced Pod:" + pod.toString());
        }
    }


    @Override
    public Boolean checkRunningSuccess(String namespace, String name) throws Exception {
        while (true) {
            log.info("获取 子pod");
            System.out.println("获取 子pod");
            V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, DEFAULT_LABEL_KEY+"=" + name, null, null, 120, false);
//            log.debug(v1PodList.toString());
            log.info(v1PodList.getItems().size() + "");
            boolean noHasNoReady=false;
            for (V1Pod item : v1PodList.getItems()) {
                DateTime dt=DateTime.now();
                val startTime=item.getStatus().getStartTime();
                Period p = new Period(item.getStatus().getStartTime(),dt, PeriodType.minutes());
                log.info("服务启动时间 {} 当前时间 {} 提交时间",startTime,dt,p);
                log.info("time after min: {} sec: {}",p.getMinutes(),p.getSeconds());
                if (!podOpera.pod_is_running(item)) {
                    log.info("子pod 未完全运行" + item.getMetadata().getName());
                    TimeUnit.SECONDS.sleep(30);
                    noHasNoReady=true;
                    continue;
                }
                //pod 运行中，并且运行时间大于5min
                if(p.getMinutes()<=2){
                    log.info("子pod 运行未超过2分钟" + item.getMetadata().getName());
                    TimeUnit.SECONDS.sleep(30);
                    noHasNoReady=true;
                    continue;
                }
            }
            if(!noHasNoReady){
                log.info("子pod都已运行");
                return true;
            }
        }
    }

    @Override
    public Boolean checkRunningSuccess(V1StatefulSet compose) throws Exception {
        return checkRunningSuccess(compose.getMetadata().getNamespace(), compose.getMetadata().getName());
    }

    @Override
    public Boolean checkDeletedSuccess(String namespace, String name) throws Exception {
        while (true) {
            log.debug("获取 子pod");
            V1PodList v1PodList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, DEFAULT_LABEL_KEY+"=" + name, null, null, 120, false);
            log.debug(v1PodList.toString());
            log.debug(v1PodList.getItems().size() + "");
            for (V1Pod item : v1PodList.getItems()) {
                log.debug(item.getMetadata().getName() + "," + item.getStatus().getPhase());
            }
            if (v1PodList.getItems().size() == 0) {
                return true;
            } else {
                TimeUnit.SECONDS.sleep(60);
            }
        }
    }

    @Override
    public Boolean checkDeletedSuccess(V1StatefulSet compose) throws Exception {
        return null;
    }


    @Override
    public Boolean delete(String namespace, String name) throws Exception {
        boolean isExist = exist(namespace, name);
        if (!isExist) {
            throw new Exception("K8s不存在相关服务，无法删除");
        }
        V1DeleteOptions v1DeleteOptions = new V1DeleteOptions();
        V1Status v1Status = appsV1Api.deleteNamespacedStatefulSet(name, namespace, null, null, 60, true, null, v1DeleteOptions);
        log.debug(v1Status.toString());
        return true;
    }

    public V1Status composeDelete(String namespace, String name) throws ApiException {
        V1DeleteOptions v1DeleteOptions = new V1DeleteOptions();
        V1Status v1Status = appsV1Api.deleteNamespacedStatefulSet(name, namespace, null, null, 60, true, null, v1DeleteOptions);
        return v1Status;
    }

}
