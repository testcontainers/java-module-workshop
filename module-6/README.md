# Can't spell Testcontainers without AI 

1. Let's look at how you can look at running LLMs in containers and what tricks, and Testcontainers API can be helpful for it. 

We're going to create tests that use an Ollama module and enhance its lifecycle so the modified images are cached. 

2. Here's an example of a code that starts the Ollama module, and accesses its API to ensure it's up and running. 
Put it into the `OllamaContainerTest` class and make it a test. 
(Note how containers are `AutoCloseable` so you can spin use `try-with-resources` with them).

```java
try (
    OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26")
) {
    ollama.start();

    String version = given().baseUri(ollama.getEndpoint()).get("/api/version").jsonPath().get("version");
    assertThat(version).isEqualTo("0.1.26");
}
```

3. By default, the Ollama Docker images don't have any models inside.
So if we want to actually use it to run any inference we need to download the model into the container.
Create another test method and download a model into the Ollama container.

```java
try (OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26")) {
    ollama.start();
    ollama.execInContainer("ollama", "pull", "all-minilm");

    String modelName = given()
            .baseUri(ollama.getEndpoint())
            .get("/api/tags")
            .jsonPath()
            .getString("models[0].name");
    assertThat(modelName).contains("all-minilm");
}
```
4. When you run this test the model is being pulled, and when we rerun the test, it will be pulled again, and again.
This is not ideal, so we can use the `commitToImage` method from the `OllamaContainer` to persist the model in a new Docker image.
See how you can use the lower level Docker Client API to work with the images: 

```java
    public void commitToImage(String imageName) {
        DockerImageName dockerImageName = DockerImageName.parse(this.getDockerImageName());
        if (!dockerImageName.equals(DockerImageName.parse(imageName))) {
            DockerClient dockerClient = DockerClientFactory.instance().client();
            List<Image> images = (List)dockerClient.listImagesCmd().withReferenceFilter(imageName).exec();
            if (images.isEmpty()) {
                DockerImageName imageModel = DockerImageName.parse(imageName);
                dockerClient.commitCmd(this.getContainerId()).withRepository(imageModel.getUnversionedPart()).withLabels(Collections.singletonMap("org.testcontainers.sessionId", "")).withTag(imageModel.getVersionPart()).exec();
            }
        }
    }
```

5. We're going to modify the lifecycle of the Ollama container from the tests to commit and use Ollama with the model pre-pulled. 
Find a suitable image name, for example: `String newImageName = "tc-ollama-allminilm";`

6. Instrument the test to check if the image with that name doesn't yet exist: 

```java
String newImageName = "tc-ollama-allminilm";
DockerClient client = DockerClientFactory.lazyClient();
List<Image> images = client.listImagesCmd().withImageNameFilter(newImageName).exec();
if (images.isEmpty()) {
    // use the code from above to create an Ollama container, pull the model, and com
        try (OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26")) {
            ollama.start();
            ollama.execInContainer("ollama", "pull", "all-minilm"); // pull the model
            
            String modelName = given()
                .baseUri(ollama.getEndpoint())
                .get("/api/tags")
                .jsonPath()
                .getString("models[0].name");
            assertThat(modelName).contains("all-minilm");
            
            ollama.commitToImage(newImageName); // commit the image
        }
}
```

7. In case the image with the model is already available (when the images list returned from the filter isn't empty), we should instantiate that: 
Add the `else` branch to the `if`-block that handles that case:

```java
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
```

8. Modify the custom image name (to get a clean stat) and run the test a few time to compare the execution time on the first run and the next ones. 

9. A nice detail of the `OllamaContainer` module is how it can automatically determine and enable the GPU support on runtimes that support it. 
This is code from the OllamaContainer class that does this. Learn how you can use Docker Client API and the `withCreateContainerCmdModifier` method to enable it. 

```java
Info info = (Info)this.dockerClient.infoCmd().exec();
Map<String, RuntimeInfo> runtimes = info.getRuntimes();
if (runtimes != null && runtimes.containsKey("nvidia")) {
    this.withCreateContainerCmdModifier((cmd) -> {
        cmd.getHostConfig().withDeviceRequests(Collections.singletonList((new DeviceRequest()).withCapabilities(Collections.singletonList(Collections.singletonList("gpu"))).withCount(-1)));
    });
}
```

10. You can also limit the resources a container has access to like CPU and memory. Here's an example of a test that checks the GC a Java process selects in constrained environments: 
Add it to your test class, run the test, explore the results. 

```java
  @CsvSource({
    ".*SerialGC.*true.*, 1791",
    ".*G1GC.*true.*, 1792"}
  )
  @ParameterizedTest
  void doSomethingWithCreate(String gcRegex, long memoryLimitInMB) throws IOException {
    try (var container =
           new GenericContainer<>("eclipse-temurin:17-jdk")
      .withCreateContainerCmdModifier(createContainerCmd -> {
          var hostConfig = new HostConfig();
          hostConfig.withMemory(memoryLimitInMB * 1024L * 1024L);
          hostConfig.withCpuCount(1L);
          createContainerCmd.withHostConfig(hostConfig);
        }
      )
      .withCommand("java -XX:+PrintFlagsFinal -version && sleep infinity")
      .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy())
    ) {
      container.start();
      var logs = container.getLogs();
      Assertions.assertThat(logs).containsPattern(gcRegex);
    }
  }
```