# Networking

Now, we are going to connect two containers. Let's use Kafka as a broker and then produce and consume
messages from it.

1. Annotate `KafkaCatTest` with `@Testcontainers`

> [!NOTE]  
> `@Tescontainers` will manage lifecycle of containers annotated with `@Container`

2. Create a network to connect both containers

```java
Network network = Network.newNetwork();
```

3. Define `KafkaContainer` and register an additional listener, which will be used by the
client

```java
@Container
KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
    .withListener(() -> "kafka:19092")
    .withNetwork(network);
```

4. Define `GenericContainer` for `kcat` to produce and read messages

```java
@Container
GenericContainer<?> kcat = new GenericContainer<>("confluentinc/cp-kcat:7.4.1")
    .withCreateContainerCmdModifier(cmd -> {
        cmd.withEntrypoint("sh");
    })
    .withCopyToContainer(Transferable.of("Message produced by kcat"), "/data/msgs.txt")
    .withNetwork(network)
    .withCommand("-c", "tail -f /dev/null");
```

5. Finally, our test will make sure the message has arrived successfully

```java
kcat.execInContainer("kcat", "-b", "kafka:19092", "-t", "msgs", "-P", "-l", "/data/msgs.txt");
String stdout = kcat
        .execInContainer("kcat", "-b", "kafka:19092", "-C", "-t", "msgs", "-c", "1")
        .getStdout();
assertThat(stdout).contains("Message produced by kcat");
```