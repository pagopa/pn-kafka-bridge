# PN Kafka Bridge

### What is this project

This project is an event-driven microservice written in Java with Spring Boot. \
Its purpose is to act as a bridge between applications external to Notification Platform (PN) that use
**Apache Kafka** (or compatible tools such as Azure EventHubs) and PN.

### Starting the project locally
To run the project locally, you need to start an Apache Kafka locally
(for example, via the following docker compose: https://developer.confluent.io/quickstart/kafka-docker/). \
Then run from the root of the project the command: `./mvnw spring-boot:run`

### Mapping Selfcare Message [OnboardingSelfCareMessage](./src/main/java/it.pagopa.pn.kafka.bridge.middleware.kafka.consumer/OnboardingSelfCareMessage.java) -> PN Payload [PnOnboardInstitutionPayload](https://github.com/pagopa/pn-model/blob/main/src/main/java/it/pagopa/pn/api/dto/events/PnOnboardInstitutionPayload.java)

|   | Selfcare Message | PN Payload     |
|---|------------------|----------------|
| Ragione Sociale del soggetto aderente: read-only ottenuto da SelfCare |     institution.description             | description    |
| Sede legale: read-only ottenuto da SelfCare  |      institution.address            | address        |
|  Partita IVA/Codice fiscale: read-only ottenuto da SelfCare |     billing.vatNumber             | taxCode        |
|  Codice IPA: read-only ottenuto da SelfCare |       institution.originId           | ipaCode        |
|  SDI: read-only ottenuto da SelfCare |        billing.recipientCode          | sdiCode        |
|  Indirizzo PEC: read-only ottenuto da SelfCare |      institution.digitalAddress            | digitalAddress |
|  Data di sottoscrizione dell’accordo: read-only ottenuto da SelfCare|        createdAt          | createdAt      |