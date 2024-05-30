package org.testcontainers.containers;

public class MongoDBContainerTest {
    public static void main(String[] args) {
        MongoDBContainer myMongo = new MongoDBContainer("mongo:7.0.9")
                .withSharding();

        myMongo.start();
        System.out.println(myMongo.getConnectionString());
        myMongo.stop();

    }
}