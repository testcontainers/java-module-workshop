package org.testcontainers.containers;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CloudflaredContainerTest {


    @Test
    public void shouldStartAndTunnelToHelloWorld() throws IOException {
        try (
                GenericContainer<?> helloworld = new GenericContainer<>(
                        DockerImageName.parse("testcontainers/helloworld:1.1.0")
                )
                        .withNetworkAliases("helloworld")
                        .withExposedPorts(8080, 8081)
                        .waitingFor(new HttpWaitStrategy())
        ) {
            helloworld.start();

            try (
                    // starting {
                    CloudflaredContainer cloudflare = new CloudflaredContainer(
                            DockerImageName.parse("cloudflare/cloudflared:2024.5.0"),
                            helloworld.getFirstMappedPort()
                    );
                    //
            ) {
                cloudflare.start();
                String url = cloudflare.getPublicUrl();
                assertThat(url).as("Public url contains 'cloudflare'").contains("cloudflare");
                System.setProperty("networkaddress.cache.ttl", "0");

                Awaitility.await().pollDelay(10, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).ignoreExceptions().untilAsserted(()-> {
                    String body = RestAssured.given().baseUri(url)
                            .get()
                            .body()
                            .print();

                    assertThat(body.trim()).as("the index page contains the title 'Hello world'").contains("Hello world");
                });

            }
        }
    }
}