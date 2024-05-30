package org.testcontainers.containers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PostgresWithTemplatesTest {

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

    @BeforeEach
    public void reset() {
        pg.reset();
    }

    @Test
    void contextLoads() {
        String s = jdbcTemplate.queryForObject("SELECT current_database();", String.class);
        System.out.println("database = " + s);

        assertThat(s).isNotEqualTo("test");

        var count = jdbcTemplate.queryForObject("SELECT count(*) from products;", Integer.class);
        assertThat(count).isPositive();
        jdbcTemplate.execute("TRUNCATE TABLE products;");

        var afterTruncate = jdbcTemplate.queryForObject("SELECT count(*) from products;", Integer.class);
        assertThat(afterTruncate).isZero();
    }

    @Test
    void contextLoads2() {
        String s = jdbcTemplate.queryForObject("SELECT current_database();", String.class);
        System.out.println("database = " + s);

        assertThat(s).isNotEqualTo("test");

        var count = jdbcTemplate.queryForObject("SELECT count(*) from products;", Integer.class);
        assertThat(count).isPositive();
        jdbcTemplate.execute("TRUNCATE TABLE products;");

        var afterTruncate = jdbcTemplate.queryForObject("SELECT count(*) from products;", Integer.class);
        assertThat(afterTruncate).isZero();
    }


}
