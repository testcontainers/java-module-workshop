# Tooling containers 

1. Sometimes, we need to run not a database or a message broker, but tools that help developers achieve something.
Let's see how we can implement a Testcontainers module for a tool. 
We'll use [Cloudflare Quick Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/do-more-with-tunnels/trycloudflare/) functionality to expose applications running locally to the public internet. 

2. Create a `CloudflaredContainer` container as usual, and make it work out of the box with the images compatible with `"cloudflare/cloudflared"`. 

```java
public class CloudflaredContainer extends GenericContainer<CloudflaredContainer> {}
```

3. Now we need to expose a host machine port, where the application we want to access from the internet will be running, with the container.
Create a constructor that takes a `DockerImageName` and an integer for the `port`.
To allow the `CloudflaredContainer` access to the host machine ports, add these lines to the constructor.

```java
withAccessToHost(true);
Testcontainers.exposeHostPorts(port);
```

4. Override the command for the image to configure cloudflare tunnel to the port we need: 

```java
withCommand("tunnel", "--url", String.format("http://host.testcontainers.internal:%d", port));
```

5. Almost universally a log line waiting strategy is a good starting point. Check the logs of the container for what could be a good line to wait for:
If you're in a rush, the result should look something like the line below: 

```java
waitingFor(Wait.forLogMessage(".*Registered tunnel connection.*", 1));
```

7. Now the tunnel will be established. Create a convenience API method to programmatically use this tunnel, for example to connect cloud service hooks to your app you're working on.

```java
public String getPublicUrl() {
        if (null != publicUrl) {
            return publicUrl;
        }
        String logs = getLogs();
        String[] split = logs.split(String.format("\\n"));
        boolean found = false;
        for (int i = 0; i < split.length; i++) {
            String currentLine = split[i];
            if (currentLine.contains("Your quick Tunnel has been created")) {
                found = true;
                continue;
            }
            if (found) {
                return publicUrl = currentLine.substring(currentLine.indexOf("http"), currentLine.indexOf(".com") + 4);
            }
        }
        throw new IllegalStateException("Didn't find public url in logs. Has container started?");
    }
```

8. Let's test this, you can create a quick main class and instantiate the `new CloudflaredContainer()` and check that it works. But in the spirit of the best practices, let's finish it with a test.
Create a `class CloudflaredContainerTest {}` class in the test classpath. 

9. We need an application that we point the tunnel into. Create a `helloworld` container like this: 

```java
GenericContainer<?> helloworld = new GenericContainer<>(DockerImageName.parse("testcontainers/helloworld:1.1.0"))
    .withNetworkAliases("helloworld")
    .withExposedPorts(8080, 8081)
    .waitingFor(new HttpWaitStrategy());

helloworld.start();
```
10. Now we have the application to point our tunnel to, instantiate the `CloudflaredContainer` and point it to the `helloworld.getFirstMappedPort()`;

11. Assuming you have a valus in the `String url = cloudflare.getPublicUrl();` use the following method to try consuming the url (and ignoring the exceptions if the tunnel is declared, but Cloudflare didn't yet provision it).
Note that we disable the DNS caching, because the domain might not be available during the first attempt, but we want to ignore it and try without cached DNS later.

```java
assertThat(url).as("Public url contains 'cloudflare'").contains("cloudflare");
System.setProperty("networkaddress.cache.ttl", "0");

Awaitility.await().pollDelay(10, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).ignoreExceptions().untilAsserted(()-> {
    String body = RestAssured.given().baseUri(url)
            .get()
            .body()
            .print();

    assertThat(body.trim()).as("the index page contains the title 'Hello world'").contains("Hello world");
});
```

🎉

Now you have a Testcontainers module with a very cool and unique functionality and can use the tunneling functionality programmatically from your apps. For the interested, you can improve the `CloudflaredContainer` to use the Cloudflare account info and not rely on the Quick tunnel making it "production" ready.  