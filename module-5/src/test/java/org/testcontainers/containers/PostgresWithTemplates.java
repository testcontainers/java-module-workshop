package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.utility.MountableFile;

public class PostgresWithTemplates extends PostgreSQLContainer {

    public static final String TESTFORREALZ = "testforrealz";

    public PostgresWithTemplates() {
        super("postgres:16-alpine");
        this.withCopyFileToContainer(MountableFile.forClasspathResource("schema.sql"),
                "/docker-entrypoint-initdb.d/");
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        this.runInitScriptIfRequired();

        snapshot();
    }

    public void snapshot() {
        try {
            ExecResult execResult = this.execInContainer("psql", "-U", "test", "-c", "ALTER DATABASE test WITH is_template = TRUE");
            System.out.println(execResult.getStdout());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        reset();

        this.withDatabaseName(TESTFORREALZ);
    }

    public void reset() {
        try {
            ExecResult execResult = this.execInContainer("psql", "-U", "test", "-c", "DROP DATABASE " + TESTFORREALZ + " with (FORCE)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            ExecResult execResult1 = this.execInContainer("psql", "-U", "test", "-c", "CREATE DATABASE " + TESTFORREALZ + " TEMPLATE test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
