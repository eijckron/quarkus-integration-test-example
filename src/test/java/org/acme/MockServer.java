package org.acme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.runner.*;
import org.junit.runners.model.*;
import org.mockserver.Version;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockServer implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
    private static final DockerImageName MOCKSERVER_IMAGE_NAME = DockerImageName.parse("mockserver/mockserver");
    private static final String MOCKSERVER_TAG = "mockserver-" + Version.getVersion();
    private static final String ALIAS = "mockserver";

    private static final int EXPOSED_PORT = 1080;

    private static MockServerClient mockServerClient = null;
    private MockServerContainer mockServerContainer = null;
    private String containerNetworkId = null;

    @Override
    public Map<String, String> start() {
        log.info("Start mockserver");
        mockServerContainer = new MockServerContainer(MOCKSERVER_IMAGE_NAME.withTag(MOCKSERVER_TAG))
                .withExposedPorts(EXPOSED_PORT)
                .withNetworkAliases(ALIAS)
                .withLogConsumer(new LogWriter("target", ALIAS));

        if (containerNetworkId != null) {
            log.info("ContainerNetworkId {} configured, using that to start mock server instance", containerNetworkId);
            mockServerContainer.setNetwork(new JoinNetwork());
        } else {
            log.info("No network passed");
        }

        mockServerContainer.start();
        log.info("Mockserver started");

        mockServerClient = new MockServerClient(mockServerContainer.getContainerIpAddress(), mockServerContainer.getMappedPort(EXPOSED_PORT));
        log.info("Mockserver client created");

        var properties = Map.of("quarkus.rest-client.message-repository.url", serviceURL("message-repository"));

        log.info("Mockserver started with properties {}", properties);

        return properties;
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        containerNetworkId = context.containerNetworkId().orElse(null);
        log.info("Use containerNetwork {}",containerNetworkId);
    }

    public static MockServerClient getMockServerClient() {
        if (mockServerClient == null) {
            throw new IllegalStateException("Mock server not initialized");
        }
        return mockServerClient;
    }

    private String serviceURL(String service) {
        String host = (containerNetworkId == null) ? mockServerContainer.getContainerIpAddress() : ALIAS;
        int port = (containerNetworkId == null) ? mockServerContainer.getMappedPort(EXPOSED_PORT) : EXPOSED_PORT;
        return String.format("http://%s:%d/%s", host, port, service);
    }

    @Override
    public void stop() {
        log.info("Stop mockserver");
        mockServerClient = null;
        if (mockServerContainer != null) {
            mockServerContainer.stop();
            mockServerContainer = null;
        }
        log.info("Mockserver stopped");
    }

    /**
     * Alternate system to start the network.
     * Has same results as using setNetworkMode though.
     */
    private class JoinNetwork implements Network {
        @Override
        public String getId() {
            log.info("JoinNetwork.getId called");
            return containerNetworkId;
        }

        @Override
        public void close() {
        }

        @Override
        public Statement apply(Statement statement, Description description) {
            return null;
        }
    }

    private static class LogWriter implements Consumer<OutputFrame> {
        private final Path logPath;

        LogWriter(String path, String containerName) {
            logPath = Path.of(path, containerName + ".log");
        }

        public void accept(OutputFrame outputFrame) {
            OutputFrame.OutputType outputType = outputFrame.getType();
            String utf8String = outputFrame.getUtf8String();

            try {
                switch (outputType) {
                    case END:
                        break;
                    case STDOUT:
                    case STDERR:
                        Files.write(logPath, utf8String.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected outputType " + outputType);
                }
            } catch (IOException ex) {
                log.info("Writing log failed: " + ex.getMessage());
                // ignore IOExceptions.
            }
        }
    }
}
