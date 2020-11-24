package com.github.cclient.k8s.compose;

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class StatefulsetOperaTest {

    List cmds = new ArrayList();
    List args = new ArrayList();
    Map<String, String> envs = new HashMap<>();
    Map<String, String> labels = new HashMap<>();
    Map<String, String> vms = new HashMap<>();
    Map<String, String> vs = new HashMap<>();
    List tolerations = new ArrayList();
    StatefulsetOpera opera = null;
    String namespace = AbstractCompose.DEFAULT_NAMESPACE;
    String name = "statefulset-nginx";
    String image = "nginx:1.19.3-alpine";

    @Before
    public void setUp() throws Exception {
        opera = new StatefulsetOpera();
        cmds = new ArrayList<String>() {
            {
                add("tail");
                add("-f");
                add("/dev/null");
            }
        };
        args = new ArrayList<String>() {
        };
        envs = new HashMap<String, String>() {
            {
            }
        };
        labels = new HashMap<String, String>() {
            {
            }
        };
        vms = new HashMap<String, String>() {
            {
            }
        };
        vs = new HashMap<String, String>() {
            {
            }
        };
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void build() {
        var pod = opera.build(namespace, name, image, envs,
                cmds, args, labels, vms, vs, tolerations, 2, 4, 1, 2);
    }

    @SneakyThrows
    @Test
    public void get() {
        opera.get(namespace, name);
    }

    @SneakyThrows
    @Test
    public void exist() {
        opera.exist(namespace, name);
    }

    @Test
    public void testExist() {
    }

    @SneakyThrows
    @Test
    public void runAsync() {
        var pod = opera.build(namespace, name, image, envs,
                args, new ArrayList<>(), labels, vms, vs, tolerations, 4, 8, 1, 2);
        opera.runAsync(pod);
    }

    @SneakyThrows
    @Test
    public void checkRunningSuccess() {
        var pod = opera.get(namespace, name);
        opera.runAsync(pod);
    }

    @Test
    public void testCheckRunningSuccess() {
    }

    @SneakyThrows
    @Test
    public void checkDeletedSuccess() {
        opera.checkDeletedSuccess(namespace, name);
    }


    @SneakyThrows
    @Test
    public void delete() {
        opera.delete(namespace, name);
    }

    @Test
    public void composeDelete() {
    }

    @SneakyThrows
    @Test
    public void deleteTillSuccessed() {
        opera.deleteTillSuccessed(namespace, name);
    }

    @SneakyThrows
    @Test
    public void runTillRunning() {
        var pod = opera.build(namespace, name, image, envs,
                args, new ArrayList<>(), labels, vms, vs, tolerations, 2, 1, 1, 1);
        opera.runTillRunning(pod);
    }

    @SneakyThrows
    @Test
    public void scale() {
        opera.scale(namespace, name, 1);
    }
}