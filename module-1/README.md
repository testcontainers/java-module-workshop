# Building a module

1. Run the command below 

```bash
docker run -e POSTGRES_PASSWORD=p@$$w0rd supabase/postgres:15.1.1.55
```

2. Let's look at the container logs and see a pattern to define the ready state. Log `database system is ready to accept connections`.

> [!NOTE]  
> Supabase is a PostgreSQL based-image.

3. Few things to consider when building this module

* The user must define the image
* `SupabaseContainer` must work with `supabase/postgres` image
* Runs using port 5432
* As identified in the previous step, ready state is defined when the string appears two times.
* Requires `POSTGRES_PASSWORD` env var

4. Let's open `SupabaseContainer` class and extend from `GenericContainer`

```java
import org.testcontainers.containers.GenericContainer;

public class SupabaseContainer extends GenericContainer<SupabaseContainer> {
    
}
```

5. Let's apply definitions from item three

```java
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public SupabaseContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DockerImageName.parse("supabase/postgres"));
    withExposedPorts(5432);
    withEnv("POSTGRES_PASSWORD", "p@$$w0rd");
    waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));
}
```

6. Now, let's add some convenient methods to access to the container

```java
public String getJdbcUrl() {
    return "jdbc:postgresql://" + getHost() + ":" + getMappedPort(5432) + "/postgres";
}

public String getUsername() {
    return "postgres";
}

public String getPassword() {
    return "p@$$w0rd";
}
```

7. Let's add a test into `SupabaseContainerTest`

```java
import org.testcontainers.utility.DockerImageName;

@Test
void test() throws SQLException {
    try (SupabaseContainer supabase = new SupabaseContainer(DockerImageName.parse("supabase/postgres:15.1.1.55"))) {
        supabase.start();
        Connection connection = DriverManager.getConnection(supabase.getJdbcUrl(), supabase.getUsername(), supabase.getPassword());
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1");
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();
        resultSet.next();
        assertThat(resultSet.getInt(1)).isEqualTo(1);
    }
}
```

8. Now, let's customize the database and password.

```java
private String password = "p@$$word";

public SupabaseContainer withPassword(String password) {
    this.password = password;
    return this;
}
```

9. Update and read the `password` field

```java
public String getPassword() {
    return this.password;
}
```

10. Let's add a new test

```java
@Test
void test2() throws SQLException {
    try (SupabaseContainer supabase = new SupabaseContainer(DockerImageName.parse("supabase/postgres:15.1.1.55"))
            .withPassword("testpassword")) {
        supabase.start();
        Connection connection = DriverManager.getConnection(supabase.getJdbcUrl(), supabase.getUsername(), supabase.getPassword());
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1");
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();
        resultSet.next();
        assertThat(resultSet.getInt(1)).isEqualTo(1);
        assertThat(supabase.getJdbcUrl()).endsWith("/test");
        assertThat(supabase.getUsername()).isEqualTo("postgres");
        assertThat(supabase.getPassword()).isEqualTo("testpassword");
    }
}
```