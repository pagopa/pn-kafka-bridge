# PN Kafka Bridge

### What is this project

This project is an event-driven microservice written in Java with Spring Boot. \
Its purpose is to act as a bridge between applications external to Notification Platform (PN) that use
**Apache Kafka** (or compatible tools such as Azure EventHubs) and PN.

### Starting the project locally
To run the project locally, you need to start an Apache Kafka locally
(for example, via the following docker compose: https://developer.confluent.io/quickstart/kafka-docker/). \
Then run from the root of the project the command: `./mvnw spring-boot:run`