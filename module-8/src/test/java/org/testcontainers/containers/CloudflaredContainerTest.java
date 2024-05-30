package org.testcontainers.containers;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CloudflaredContainerTest {

    @Test
    public void shouldStartAndTunnelToHelloWorld() throws IOException {

    }
}