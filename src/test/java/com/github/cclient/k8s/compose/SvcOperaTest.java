package com.github.cclient.k8s.compose;

import lombok.SneakyThrows;
import org.junit.Test;

public class SvcOperaTest {
    String namespace = AbstractCompose.DEFAULT_NAMESPACE;
    String name = "pod-nginx";
    SvcOpera svcOpera = new SvcOpera();

    @Test
    public void build() {
        var s = svcOpera.build(namespace, name, 8888);
        System.out.println(s);
    }

    @SneakyThrows
    @Test
    public void deploy() {
        var s = svcOpera.build(namespace, name, 8888);
        var r = svcOpera.deployAndGetNodePort(s);
        System.out.println(r);
    }

    @SneakyThrows
    @Test
    public void delete() {
        svcOpera.delete(namespace, name);
    }

    @SneakyThrows
    @Test
    public void exist() {
        var end = svcOpera.exist(namespace, name);
        System.out.println(end);
    }


}