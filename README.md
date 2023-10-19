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
      "S":"<message.created>"
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
      "S":"<message.lastUpdate>"
    },
    "sdiCode":{
      "S":"<message.billing.recipientCode>"
    },
    "status":{
      "S":"<message.status>"
    },
    "taxCode":{
      "S":"<message.billing.vatNumber>"
    }
    }   
    ```