package org.testcontainers.ollama;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class OllamaContainerTest {

}