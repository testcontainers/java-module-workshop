# Spring Boot Service Connections

In Spring Boot 3.1, the notion of `ConnectionDetails` and `ServiceConnection` were introduced.
WireMock is not supported by Spring Boot but we can build our own implementation.

1. Run `TestcontainersWiremockExampleApplicationTests`

2. Create `WireMockContainerConnectionDetailsFactory`

```java
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.wiremock.integrations.testcontainers.WireMockContainer;

class WireMockContainerConnectionDetailsFactory extends ContainerConnectionDetailsFactory<WireMockContainer, GHConnectionDetails> {
    WireMockContainerConnectionDetailsFactory() {
    }

    protected GHConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<WireMockContainer> source) {
        return new WireMockContainerConnectionDetails(source);
    }

    private static final class WireMockContainerConnectionDetails extends ContainerConnectionDetails<WireMockContainer> implements GHConnectionDetails {
        private WireMockContainerConnectionDetails(ContainerConnectionSource<WireMockContainer> source) {
            super(source);
        }


        @Override
        public String url() {
            return getContainer().getBaseUrl();
        }

        @Override
        public String token() {
            return "test-token";
        }
    }
}
```

3. Update `TestcontainersWiremockExampleApplicationTests`

Remove

```java
@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    registry.add("github.url", wireMock::getBaseUrl);
    registry.add("github.token", () -> "test");
}
```

Add `@ServiceConnection` to `WireMockContainer`

```java
...
@ServiceConnection
static WireMockContainer wireMock = new WireMockContainer("wiremock/wiremock:3.2.0-alpine")
...		
```

4. Run `TestcontainersWiremockExampleApplicationTests` and see the test failure

```
No ConnectionDetails found for source '@ServiceConnection source for TestcontainersWiremockExampleApplicationTests.wireMock'
```

5. Let's add the content below to `src/main/resources/META-INF/spring.factories`

```
org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory=\
com.example.testcontainerswiremockexample.WireMockContainerConnectionDetailsFactory
```

6. Run `TestcontainersWiremockExampleApplicationTests` successfully
