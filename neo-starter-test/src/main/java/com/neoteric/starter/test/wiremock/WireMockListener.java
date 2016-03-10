package com.neoteric.starter.test.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.neoteric.starter.test.TestBeanUtils;
import com.neoteric.starter.test.wiremock.ribbon.RibbonStaticServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.io.IOException;
import java.net.ServerSocket;

public class WireMockListener extends AbstractTestExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockListener.class);
    private static final String RIBBON_SERVER_LIST = "ribbonServerList";

    private WireMockServer server;
    private int port;

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        if (testContext.getTestClass().getAnnotation(WireMockTest.class) == null) {
            return;
        }

        ConfigurableEnvironment environment = (ConfigurableEnvironment)testContext.getApplicationContext().getEnvironment();
        EnvironmentTestUtils.addEnvironment(environment, "ribbon.eureka.enabled=false");

        port = getFreeServerPort();
        server = new WireMockServer(port);
        TestBeanUtils.registerSingleton(testContext, RIBBON_SERVER_LIST, new RibbonStaticServer());
        RibbonStaticServer.port = port;
        LOG.info("WireMock started on port {}\n", port);

    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        if (server == null) {
            return;
        }
        server.start();
        WireMock.configureFor(port);
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        if (server == null) {
            return;
        }
        server.resetRequests();
        server.resetMappings();
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        if (server == null) {
            return;
        }
        TestBeanUtils.destroySingleton(testContext, RIBBON_SERVER_LIST);
        server.stop();

        ConfigurableEnvironment environment = (ConfigurableEnvironment)testContext.getApplicationContext().getEnvironment();
        MapPropertySource test = (MapPropertySource) environment.getPropertySources().get("test");
        test.getSource().remove("ribbon.eureka.enabled");
    }

    private int getFreeServerPort() throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(0);
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            LOG.error("Socket Exception while opening socket");
            throw e;
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                LOG.error("Socket Exception while closing socket", e);
            }
        }
    }
}
