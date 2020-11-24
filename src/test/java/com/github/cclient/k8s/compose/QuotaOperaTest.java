package com.github.cclient.k8s.compose;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class QuotaOperaTest {

    QuotaOpera opera;

    @Before
    public void setUp() throws Exception {
        opera = new QuotaOpera();
    }

    @SneakyThrows
    @Test
    public void getQuota() {
        val res = opera.getQuota(AbstractCompose.DEFAULT_NAMESPACE);
        log.info(res.toString());
    }

    @Test
    public void testGetQuota() {
    }

    @SneakyThrows
    @Test
    public void getQuotaNum() {
        val res = opera.getQuotaNum(AbstractCompose.DEFAULT_NAMESPACE);
        log.info(res.toString());
    }

    @Test
    public void testGetQuotaNum() {
    }

    @SneakyThrows
    @Test
    public void getQuotaNumSplit() {
        val res = opera.getQuotaNumSplit(AbstractCompose.DEFAULT_NAMESPACE);
        log.info(res.toString());
    }
}