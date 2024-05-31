# Advanced lifecycle for Databases (Postgres Template databases)

1. Imagine you need to work with the database and want to reset it to a given state during the run.
There are many ways to achieve this, but in this step we'll work with Postgres, and use the [Template databases](https://www.postgresql.org/docs/current/manage-ag-templatedbs.html) functionality to show how can you work with the container and expose executing shell commands in it to your tests.
Other databases can also have similar functionality under different names. 

2. Databases in Postgres can be declared as a template with the following alter database command, and new databases created from a template by specifying it like this:
```sql 
ALTER DATABASE test WITH is_template = TRUE;
CREATE DATABASE test1 TEMPLATE test;
```

3. Let's extend `PostgresContainer` class and create a `PostgresWithTemplates` class. For simplicity, let's hardcode the schema file that initializes the database schema with our data right into the constructor.
Here's a snippet that prepares the class for our needs: we specify the name of the database we will expose to the application and override the `containerIsStarted` method for adding code to the lifecycle. 

```java
public static final String ACTUAL_DATABASE_NAME = "realtest";

public PostgresWithTemplates() {
    super("postgres:16-alpine");
    this.withCopyFileToContainer(MountableFile.forClasspathResource("schema.sql"),
            "/docker-entrypoint-initdb.d/");
}

@Override
protected void containerIsStarted(InspectContainerResponse containerInfo) {
    this.runInitScriptIfRequired();
}
``` 

4. Now, we need to expose running the shell commands that tell Postgres to mark a database as a template as a Java method: 
Add `snapshot` to the PostgresWithTemplates class:

```java
public void snapshot() {
    try {
        ExecResult execResult = this.execInContainer("psql", "-U", "test", "-c", "ALTER DATABASE test WITH is_template = TRUE");
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    reset();

    this.withDatabaseName(ACTUAL_DATABASE_NAME);
}
```

5. Add the `reset` method as well, it will drop the database we expose to the users and create a new copy of our template database: 

```java
public void reset() {
    try {
        ExecResult execResult = this.execInContainer("psql", "-U", "test", "-c", "DROP DATABASE " + ACTUAL_DATABASE_NAME + " with (FORCE)");
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    try {
        ExecResult execResult1 = this.execInContainer("psql", "-U", "test", "-c", "CREATE DATABASE " + ACTUAL_DATABASE_NAME + " TEMPLATE test");
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
} 
```

6. Add the call to `snapshot` as the last action to the `containerIsStarted` method. 

7. Let's test how it works now. Setup the test `PostgresWithTemplatesTest` by creating the `PostgresWithTemplates` container, wiring the `JDBCTemplate`, and configuring the context to use our container:

```java
static PostgresWithTemplates pg = new PostgresWithTemplates();

static {
    pg.start();
}

@Autowired
JdbcTemplate jdbcTemplate;

@DynamicPropertySource
public static void setup(DynamicPropertyRegistry reg) {
    reg.add("spring.datasource.username", pg::getUsername);
    reg.add("spring.datasource.password", pg::getPassword);
    reg.add("spring.datasource.url", pg::getJdbcUrl);
}
```

8. Add a test that verifies the database has our information in it. Note how the test truncates the database in the end and checks that there's no data after the execution.

```java
@Test
void contextLoads() {
    String s = jdbcTemplate.queryForObject("SELECT current_database();", String.class);

    assertThat(s).isNotEqualTo("test");

    var count = jdbcTemplate.queryForObject("SELECT count(*) from products;", Integer.class);
    assertThat(count).isPositive();
    jdbcTemplate.execute("TRUNCATE TABLE products;");

    var afterTruncate = jdbcTemplate.queryForObject("SELECT count(*) from products;", Integer.class);
    assertThat(afterTruncate).isZero();
} 
```

9. Duplicate the test method, call it `contextLoads2`, and run all tests in the class. One of them should fail, because the database is modified during the first test run, and doesn't recover.

10. Add the call to reset the database state between the tests. It should fix the corrupted database state issue and both tests should pass now.
```java
@BeforeEach
public void reset() {
    pg.reset();
}
```

🎉