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

public class PodOperaTest {
    List cmds = new ArrayList();
    List args = new ArrayList();
    Map<String, String> envs = new HashMap<>();
    Map<String, String> labels = new HashMap<>();
    Map<String, String> vms = new HashMap<>();
    Map<String, String> vs = new HashMap<>();
    List tolerations = new ArrayList();
    PodOpera opera = null;
    String namespace = AbstractCompose.DEFAULT_NAMESPACE;
    String name = "pod-nginx";
    String image = "nginx:1.19.3-alpine";

    @Before
    public void setUp() throws Exception {
        opera = new PodOpera();
        cmds = new ArrayList<String>() {
            {
                add("tail");
                add("-f");
                add("/dev/null");
            }
        };
        args = new ArrayList<String>() {
            {
            }
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
    public void buildYaml() {
    }

    @Test
    public void checkRunningFinished() {
    }

    @Test
    public void testCheckRunningFinished() {
    }

    @SneakyThrows
    @Test
    public void deleteTillSuccessed() {
        opera.deleteTillSuccessed(namespace, name);
    }

    @SneakyThrows
    @Test
    public void runTillRunning() {
        opera.runTillRunning(namespace, name, image, envs,
                args, new ArrayList<>(), labels, vms, vs, tolerations, 100, 1, 1, 0);
    }

    @Test
    public void build() {
    }

    @Test
    public void get() {
    }

    @Test
    public void exist() {
    }

    @Test
    public void testExist() {
    }

    @Test
    public void runAsync() {
    }

    @Test
    public void checkRunningSuccess() {
    }

    @Test
    public void testCheckRunningSuccess() {
    }

    @Test
    public void checkDeletedSuccess() {
    }

    @Test
    public void testCheckDeletedSuccess() {
    }

    @Test
    public void composeDelete() {
    }

    @Test
    public void delete() {
    }
}