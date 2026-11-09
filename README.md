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

### Delegations flow

The bridge also consumes the SelfCare delegations topic (`sc-delegations`, with a dedicated credential and a dedicated bootstrap servers configuration) and publishes the events of the delegations to the PT on the SQS queue `<ProjectName>-kafka_bridge_delegations`, as [PnPtDelegationEvent](https://github.com/pagopa/pn-model/blob/main/src/main/java/it/pagopa/pn/api/dto/events/PnPtDelegationEvent.java).

* Filter: only the records with `type = PT` are processed; if `PN_KAFKA_BRIDGE_SELFCARE_PN_PRODUCT_ID` is set, the records of other products are discarded.
* Validation: only the fields required by the feature are checked: `from`, `to`, `delegationId`, `status`, `institutionFromName` and `fromTaxCode` must be present, `status` must be `ACTIVE` or `DELETED`, `createdAt`, `updatedAt` and `closedAt`, when present, must be valid dates with offset. The other fields are not validated. An invalid event is logged as error and not published.
* Message header: `iun` is the `delegationId`, `eventId` is `<delegationId>_<sourceEventId>`, `eventType` is `PT_DELEGATION_CHANGED`, `publisher` is `KAFKA_BRIDGE`.

Kafka configuration is per flow: each flow has its own `KafkaClusterProperties` (`pn.kafka-bridge.onboarding-contracts` and `pn.kafka-bridge.delegations`) with bootstrap servers, group id, topic, SASL credential (`sasl-jaas-password`) and the Kafka consumer properties under `properties.*` (deserializers, commit and poll settings). \
The group id is used as `id` of the `@KafkaListener`, so it is also the consumer group: the two flows must have different values, otherwise the application does not start.

| Property | Environment variable | Flow |
|---|---|---|
| `pn.kafka-bridge.onboarding-contracts.topic-name` | `PN_KAFKA_BRIDGE_ONBOARDING_CONTRACTS_TOPIC_NAME` | onboarding |
| `pn.kafka-bridge.onboarding-contracts.group-id` | `PN_KAFKA_BRIDGE_ONBOARDING_CONTRACTS_GROUP_ID` | onboarding |
| `pn.kafka-bridge.onboarding-contracts.bootstrap-servers` | `PN_KAFKA_BRIDGE_ONBOARDING_CONTRACTS_BOOTSTRAP_SERVERS` | onboarding |
| `pn.kafka-bridge.sqs-topics.onboarding-input` | `PN_KAFKA_BRIDGE_SQS_TOPICS_ONBOARDING_INPUT` | onboarding |
| `pn.kafka-bridge.delegations.topic-name` | `PN_KAFKA_BRIDGE_DELEGATIONS_TOPIC_NAME` | delegations |
| `pn.kafka-bridge.delegations.group-id` | `PN_KAFKA_BRIDGE_DELEGATIONS_GROUP_ID` | delegations |
| `pn.kafka-bridge.delegations.bootstrap-servers` | `PN_KAFKA_BRIDGE_DELEGATIONS_BOOTSTRAP_SERVERS` | delegations |
| `pn.kafka-bridge.delegations.properties.auto.commit.interval.ms` | `PN_KAFKA_BRIDGE_DELEGATIONS_PROPERTIES_AUTO_COMMIT_INTERVAL_MS` | delegations |
| `pn.kafka-bridge.delegations.sasl-jaas-password` | `KAFKA_DELEGATIONS_SASL_JAAS_PASSWORD` | delegations |
| `pn.kafka-bridge.delegations.auth-exception-retry-interval` | | delegations (default `60s`) |
| `pn.kafka-bridge.sqs-topics.delegations-input` | `PN_KAFKA_BRIDGE_SQS_TOPICS_DELEGATIONS_INPUT` | delegations |

Before the deploy, SelfCare must enable the PN key and the consumer group on the topic, the key `KafkaDelegationsSaslJaasConfig` must exist in the secret `pn-KafkaBridge-Secrets` and the queue `<ProjectName>-kafka_bridge_delegations` must exist. \
The parameter `KafkaDelegationsGroupId` (environment variable `PN_KAFKA_BRIDGE_DELEGATIONS_GROUP_ID`) has no default and must be set to the consumer group authorized by SelfCare, otherwise the stack deploy fails. \
The parameter `KafkaDelegationsBootstrapServers` (environment variable `PN_KAFKA_BRIDGE_DELEGATIONS_BOOTSTRAP_SERVERS`) must be set to the Event Hub namespace of the delegations topic, which must allow the IPs of the environment.

### Troubleshooting

#### Scrivere direttamente un evento sulla tabella DynamoDB

A seguito di un errore, ad esempio un errore sul contenuto di un messaggio ricevuto in input, è possibile sopperire
all'errore ed effettuare l'onboarding dell'institution scrivendo direttamente il record sulla tabella DynamoDB. \
Questi sono i passi da seguire:
1. Individuare il messaggio ricevuto in input mediante la loggatura: 
    ```java
   "[aaaaaaaaaaaaaaaaaaaaaa] Received message from topic: sc-contracts, with value:
     OnboardingSelfCareMessage(billing=OnboardingSelfCareMessage.Billing(recipientCode=UUUUUU, vatNumber=00000000000, 
   publicServices=false), contentType=application/json, fileName=, filePath=null, id=ccccccccccccccccccccc, 
   institution=OnboardingSelfCareMessage.Institution(address=via Roma 3, 
   description=Comune di Prova, digitalAddress=prova@pec.it, institutionType=GSP, origin=IPA, 
   originId=slrs, taxCode=00000000000,), internalIstitutionID=aaaaaaaaaaaaaaaaaaaaaa, 
   onboardingTokenId=bbbbbbbbbbbbbbbbbbbbbbbb, product=prod-prova, state=ACTIVE, createdAt=2023-10-18T08:03:43.546545Z, 
   updatedAt=2023-10-18T08:03:43.546545Z, pricingPlan=null, zipCode=null)"
    ```
2.  Prendendo i valori del log precedente, scrivere sulla tabella DynamoDB `pn-OnboardInstitutions` il seguente record:
    ```json
    {
    "id":{
      "S":"<message.internalIstitutionID>"
    },
    "address":{
      "S":"<message.institution.address>"
    },
    "created":{
      "S":"<message.createdAt>"
    },
    "description":{
      "S":"<message.institution.description>"
    },
    "digitalAddress":{
      "S":"<message.institution.digitalAddress>"
    },
    "externalId":{
      "S":"<message.onboardingTokenId>"
    },
    "ipaCode":{
      "S":"<message.institution.originId>"
    },
    "lastUpdate":{
      "S":"<message.updatedAt>"
    },
    "sdiCode":{
      "S":"<message.billing.recipientCode>"
    },
    "status":{
      "S":"<message.state>"
    },
    "taxCode":{
      "S":"<message.institution.taxCode>"
    },
    "rootId":{
      "S":"IF (message.institution.rootParent=null OR message.institution.rootParent.id=null) THEN <message.internalIstitutionID> ELSE <message.institution.rootParent.id>"
    },
    "rootDescription":{
      "S":"<message.institution.rootParent.description>"
    },
    "rootIpaCode":{
      "S":"<message.institution.rootParent.originId>"
    },
    "zipCode":{
      "S":"<message.zipCode"
    }
    }   
    ```