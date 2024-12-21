# Prompt for a decoupled, event-driven core banking system

## Context

You are an expert architect with a lot of context on fintechs and the regular constraits of a core banking system for a continent scale digital bank, built for scalability and resilience from scract. You are an expert on the Java 21+ And Spring Boot 3.2+ ecosystem. You are also an expert on Engineering Excellence (Quality, CI-CD, DORA, Automation) and Developer Experience.

## Requirements

1. Write a Spring Boot app, latest version that you know, leveraging Spring Modulith latest version, having two modules (that later could be easily broken down / digested as domains) Accounts and Transfers. This application should have minimal documentation and handy automations on its pom.xml or Makefile. The modules should talk to each other using Spring's event bus and Modulith's asynchronous support. An unit test that asserts that modularity and outputs a C4 Diagram should be present, as per the native feature. The app should also leverage Java 21+ support for virtual threads, by adding the proper configuration to application.yml
2. The Java application should use the latest and greatest for Data Oriented Programming
3. Let's use Spring Boot's support for GraphQL, instead of regular Spring Web. If possible, let's add a third module that would be the federated gateway, calling internal domains account and transfers as needed
4. Package the application by feature, meaning that instead of having controller/ service/ repository/ layers, we should have all the pertaining files into packages like accounts.onboard, accounts.activation, transactions.create, and etc
5. The Spring Boot app should use Spring Data JDBC to support its event schema. The schema should be migrated using some db migration tool. Output a basic, but well-design data schema/model to support accounts and transactions and best practices for calculating "extratos" (including deciding whether that should live in the same DB or we should CQRS + Event Source it. Explain why)
6. This Postgresql, running locally on a docker container, should have a CDC that we can connect two things to:
   1. Debezium, pushing account and transactions CRUD related events to a Redpanda running locally (leveraging its Kafka API)
   2. Bento (the fork after Benthos was acquired by Redpanda) should be also connecting to pgoutput's CDC and bridging the same data to a NATS server on docker and to a Redis Stream on docker
7. We should have other "consumer" data pipelines that read data from Kafka, Bento and NATS and transform the data into useful use cases leveraging Redis Modules (choose wisely which data source to use, from Redis possibilities like Redis key-value, RedisJSON, RedisSearch):
   1. Preparing database that is optimized for "emitir extrato de conta (PTBR)". Not using triggers, using things that are proven to be highly scalable. If another DB type rather than postgres is better for that, so be it!
   2. Loyalty Accrual and Redeem
   3. Push Notifications
   4. Report Pre-generation
   5. Enrich and vectorize/embed data using AI embeddings into a pgvector db (specific for this case)
   6. Fraud detection / user behavior mapping
8. We should have two versions of consumers: Ephemeral NATS consumers and Bento consumers
9. Create all the handy automation for local unit testing / validating each meaningful step of the process
10. Output a mermaid sequence Diagram of some of the flows
11. Output a Mermaid diagram that resembles a C4 Component diagram
12. The output should be incremental and produce as much of working code as possible. No samples, no comments abstracting things away. I want to see real, working code. Organize it as a tutorial, be as detailed and thorough per step as possible, I don't want to see "we could / should do". Everything that you mention should be demonstrated somehow. Go thoroughly on a given step, then ask me for the next one
13. Always keep a tracker TOC before the output
14. The first step of the tutorial should be drawing a mermaid diagram using C4's concepts for all the involved Containers. The C4 Diagram should be as beautiful as possible, with limited lines overlapping each other
15. The second step should be setting up the needed infrastructure via docker
16. The third step should be setting up the main Spring Modulith
17. From there, ask me which next step from the remaining steps that you calculate do I prefer. Update the TOC acccordingly from then on as per my needs
18. Observability should be handled using a single Grafana LGTM resilient deployment on Docker
19. After finishing everything, let's create a local k8s cluster and let's use it as a "staging" environment. We will some "CI-CD" scripts running locally, we can maybe use a Makefile to orchestrate that. Come up with some ideas when time comes for this element of the TOC.
