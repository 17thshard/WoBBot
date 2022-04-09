FROM ubuntu:20.04

RUN apt update && apt upgrade -y && apt install -y maven

RUN useradd --shell /bin/sh --create-home wobbot


WORKDIR /bot
COPY pom.xml pom.xml
COPY src src
COPY .git .git
RUN chown wobbot -R /bot

USER wobbot

RUN mvn install

ENTRYPOINT mvn exec:java
