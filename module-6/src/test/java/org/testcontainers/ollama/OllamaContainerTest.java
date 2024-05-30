package org.testcontainers.ollama;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class OllamaContainerTest {

    @Test
    public void withDefaultConfig() {
        try (
                OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26")

        ) {
            ollama.start();

            String version = given().baseUri(ollama.getEndpoint()).get("/api/version").jsonPath().get("version");
            assertThat(version).isEqualTo("0.1.26");
        }
    }

    @Test
    public void downloadModelAndCommitToImage() throws IOException, InterruptedException {
        String newImageName = "tc-ollama-allminilm";
        DockerClient client = DockerClientFactory.lazyClient();
        List<Image> images = client.listImagesCmd().withImageNameFilter(newImageName).exec();
        if (images.isEmpty()) {
            try (OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26")) {
                ollama.start();
                ollama.execInContainer("ollama", "pull", "all-minilm");

                String modelName = given()
                        .baseUri(ollama.getEndpoint())
                        .get("/api/tags")
                        .jsonPath()
                        .getString("models[0].name");
                assertThat(modelName).contains("all-minilm");
                ollama.commitToImage(newImageName);
            }
        } else {
            try (
                    OllamaContainer ollama = new OllamaContainer(
                            DockerImageName.parse(newImageName)
                                    .asCompatibleSubstituteFor("ollama/ollama")
                    )
            ) {
                ollama.start();
                String modelName = given()
                        .baseUri(ollama.getEndpoint())
                        .get("/api/tags")
                        .jsonPath()
                        .getString("models[0].name");
                assertThat(modelName).contains("all-minilm");
            }
        }
    }
}