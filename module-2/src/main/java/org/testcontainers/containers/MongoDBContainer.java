package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.NonNull;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

public class MongoDBContainer extends GenericContainer<MongoDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mongo");
    private static final String DEFAULT_TAG = "7.0.9";

    private static final int CONTAINER_EXIT_CODE_OK = 0;
    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 60;

    private static final String MONGODB_DATABASE_NAME_DEFAULT = "test";

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    private boolean shardingEnabled;

    public MongoDBContainer(@NonNull final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MongoDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    @Override
    MongoDBContainerDef createContainerDef() {
        return new MongoDBContainerDef();
    }

    @Override
    MongoDBContainerDef getContainerDef() {
        return (MongoDBContainerDef) super.getContainerDef();
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        if (this.shardingEnabled) {
            copyFileToContainer(MountableFile.forClasspathResource("/sharding.sh", 0777), STARTER_SCRIPT);
        }
    }

    /**
     * Enables sharding on the cluster
     *
     * @return this
     */
    public MongoDBContainer withSharding() {
        this.shardingEnabled = true;
        getContainerDef().withSharding();
        return this;
    }

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

    /**
     * Gets a connection string url, unlike {@link #getReplicaSetUrl} this does not point to a
     * database
     * @return a connection url pointing to a mongodb instance
     */
    public String getConnectionString() {
        return String.format("mongodb://%s:%d", getHost(), getMappedPort(MongoDBContainerDef.MONGODB_INTERNAL_PORT));
    }

    /**
     * Gets a replica set url for the default {@value #MONGODB_DATABASE_NAME_DEFAULT} database.
     *
     * @return a replica set url.
     */
    public String getReplicaSetUrl() {
        return getReplicaSetUrl(MONGODB_DATABASE_NAME_DEFAULT);
    }

    /**
     * Gets a replica set url for a provided <code>databaseName</code>.
     *
     * @param databaseName a database name.
     * @return a replica set url.
     */
    public String getReplicaSetUrl(final String databaseName) {
        if (!isRunning()) {
            throw new IllegalStateException("MongoDBContainer should be started first");
        }
        return getConnectionString() + "/" + databaseName;
    }

    private String[] buildMongoEvalCommand(final String command) {
        return new String[] {
                "sh",
                "-c",
                "mongosh mongo --eval \"" + command + "\"  || mongo --eval \"" + command + "\"",
        };
    }

    private void checkMongoNodeExitCode(final Container.ExecResult execResult) {
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format("An error occurred: %s", execResult.getStdout());
            throw new ReplicaSetInitializationException(errorMessage);
        }
    }

    private String buildMongoWaitCommand() {
        return String.format(
                "var attempt = 0; " +
                        "while" +
                        "(%s) " +
                        "{ " +
                        "if (attempt > %d) {quit(1);} " +
                        "print('%s ' + attempt); sleep(100);  attempt++; " +
                        " }",
                "db.runCommand( { isMaster: 1 } ).ismaster==false",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS,
                "An attempt to await for a single node replica set initialization:"
        );
    }

    private void checkMongoNodeExitCodeAfterWaiting(final Container.ExecResult execResultWaitForMaster) {
        if (execResultWaitForMaster.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format(
                    "A single node replica set was not initialized in a set timeout: %d attempts",
                    AWAIT_INIT_REPLICA_SET_ATTEMPTS
            );
            throw new ReplicaSetInitializationException(errorMessage);
        }
    }

    private void initReplicaSet(boolean reused) throws IOException, InterruptedException {
        if (reused && isReplicationSetAlreadyInitialized()) {
            return;
        }
        else {
            final ExecResult execResultInitRs = execInContainer(buildMongoEvalCommand("rs.initiate();"));
            checkMongoNodeExitCode(execResultInitRs);
            final ExecResult execResultWaitForMaster = execInContainer(buildMongoEvalCommand(buildMongoWaitCommand()));

            checkMongoNodeExitCodeAfterWaiting(execResultWaitForMaster);
        }
    }

    public static class ReplicaSetInitializationException extends RuntimeException {

        ReplicaSetInitializationException(final String errorMessage) {
            super(errorMessage);
        }
    }

    private boolean isReplicationSetAlreadyInitialized() throws IOException, InterruptedException {
        // since we are creating a replica set with one node, this node must be primary (state = 1)
        final Container.ExecResult execCheckRsInit = execInContainer(
                buildMongoEvalCommand("if(db.adminCommand({replSetGetStatus: 1})['myState'] != 1) quit(900)")
        );
        return execCheckRsInit.getExitCode() == CONTAINER_EXIT_CODE_OK;
    }

    private static class MongoDBContainerDef extends ContainerDef {

        private static final int MONGODB_INTERNAL_PORT = 27017;

        MongoDBContainerDef() {
            addExposedTcpPort(MONGODB_INTERNAL_PORT);
            setCommand("--replSet", "docker-rs");
            setWaitStrategy(Wait.forLogMessage("(?i).*waiting for connections.*", 1));
        }

        void withSharding() {
            setCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
            setWaitStrategy(Wait.forLogMessage("(?i).*mongos ready.*", 1));
            setEntrypoint("sh");
        }
    }
}
