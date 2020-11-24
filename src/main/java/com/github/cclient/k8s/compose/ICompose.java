package com.github.cclient.k8s.compose;

import java.util.List;
import java.util.Map;

public interface ICompose<T, R, L> {

    T build(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas);

    String buildYaml(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas);

    T get(String namespace, String name) throws Exception;

    Boolean exist(String namespace, String name) throws Exception;

    Boolean exist(T compose) throws Exception;

    T runAsync(T pod) throws Exception;

    T runAsync(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) throws Exception;

    Boolean checkRunningSuccess(String namespace, String name) throws Exception;

    Boolean checkRunningSuccess(T compose) throws Exception;

    Boolean checkRunningFinished(String namespace, String name) throws Exception;

    Boolean checkRunningFinished(T compose) throws Exception;

    Boolean checkDeletedSuccess(String namespace, String name) throws Exception;

    Boolean checkDeletedSuccess(T compose) throws Exception;


    Boolean delete(String namespace, String name) throws Exception;

    Boolean deleteTillSuccessed(String namespace, String name) throws Exception;

    Boolean runTillRunning(T pod) throws Exception;

    Boolean runTillRunning(String namespace, String name, String image, Map<String, String> envs, List<String> cmds, List<String> args, Map<String, String> labels, Map<String, String> volumeMounts, Map<String, String> volumes, List<Map<String, String>> tolerations, Integer request_cpu, Integer request_mem, Integer request_gpu, Integer replicas) throws Exception;

}
