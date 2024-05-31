# Overriding the Default commands

1. Sometimes, we need to use a custom command for the image. Either to enable some best practices for using the containers in integration tests or just to satisfy configuration selected by the user. 
In this step we'll look at running MongoDB in a container, and enabling custom configuration for it.
The implementation of the MongoDB container in the `MongoDBContainer.java` class doesn't include support for sharding. 
The current implementation adds the customization to the image command with `setCommand` method. Let's add the sharding support!
```java
setCommand("--replSet", "docker-rs");
```

2. To start MongoDB with sharding, we need to do a bit of work. You can look into what the script does in the `sharding.sh` file. 

3. Before we start implementing the sharding support in our `MongoDBContainer`, lets verify the initial implementation works. 
Run the application in the `MongoDBContainerTest` which starts a container, prints the connection url, and stops it. 

4. Add the `shardingEnabled` field to the `MongoDBContainer` class: 
```java
private boolean shardingEnabled;
```

5. Modify the `containerIsStarted` method to only initialize the ReplicaSet if we're not doing sharding: 
```java
@Override
protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
    if (!this.shardingEnabled) {
        try {
            initReplicaSet(reused);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
```

6. Add the `withSharding()` method to be able to configure the MongoDBContainer with sharding. Note that `MongoDBContainerDef` implementation doesn't do anything with the sharding info (the `withSharding()` method is empty).
```java
public MongoDBContainer withSharding() {
    this.shardingEnabled = true;
    getContainerDef().withSharding();
    return this;
}
```

7. When sharding is enabled, we want to copy the `sharding.sh` file to the container and make it the entrypoint. Add the field to specify the script location in the container:
```java
private static final String STARTER_SCRIPT = "/testcontainers_start.sh";
```

8. Override the `containerIsStarting` method to place the script into the container during the startup lifecycle phase: 
```java
@Override
protected void containerIsStarting(InspectContainerResponse containerInfo) {
    if (this.shardingEnabled) {
        copyFileToContainer(MountableFile.forClasspathResource("/sharding.sh", 0777), STARTER_SCRIPT);
    }
}
```

9. Finally, enhance the `MongoDBContainerDef.withSharding()` method to override the entrypoint and make container run `sharding.sh` on startup:

```java
void withSharding() {
    setCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
    setWaitStrategy(Wait.forLogMessage("(?i).*mongos ready.*", 1));
    setEntrypoint("sh");
}
```

10. Add the `withSharding()` call to the `MongoDBContainerTest` container config, and run the test:
```java
MongoDBContainer myMongo = new MongoDBContainer("mongo:7.0.9")
    .withSharding();
```

11. Stop the test before the container is stopped and open the terminal into the `MongoDBContainer`. If using Testcontainers Desktop, use the menu item to freeze containers and open the terminal into the container.
Otherwise find the container ID in the `docker ps` output, and execute: `docker exec -it $ID /bin/sh`.  
In the container run the following commands and check if the sharding is enabled.  
```shell
mongosh 
use admin
db.adminCommand({ listShards: 1 })
```

🎉We learned a nice trick to run a custom starting script for the image without building custom images.
