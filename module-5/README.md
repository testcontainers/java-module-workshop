# Advanced lifecycle for Databases (Postgres Template databases)

1. Imagine you need to work with the database and want to reset it to a given state during the run.

```bash

```

2. Let's looks at the Postgres database and how you can use Template databases for snapshotting the state. Template databases are marked with the `is_template = true`, and when you create new database you can specify which template to use. 

This allows us to create an advanced lifecycle database module which encapsulates the template databases functionality and provides convenience API to trigger it from your tests. 
There are some restrictions like the template database cannot have active connections when you copy it, so it helps to hide the complexity from the end used behind a Testcontainers module.


3. Let's extend `PostgresContainer` class and create a `PostgresWithTemplates` class. 

```