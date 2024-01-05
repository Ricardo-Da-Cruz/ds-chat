FROM openjdk:11
MAINTAINER baeldung.com
COPY /target/ds-chat.jar peer.jar
COPY words.txt words.txt
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install tcpdump -y && \
    apt-get install iputils-ping -y


ENTRYPOINT ["java", "-jar", "./peer.jar","127.17.0.2","127.17.0.3","127.17.0.4","127.17.0.5"]