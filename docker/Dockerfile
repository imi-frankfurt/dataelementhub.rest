FROM openjdk:18-jdk-alpine
EXPOSE 8090
COPY docker/start.sh .
COPY docker/dehub-update-ca-certificates /etc/sudoers.d/
RUN apk add sudo
RUN chmod +x ./start.sh
RUN addgroup -S dehub && adduser -S dehub -G dehub
RUN mkdir /var/exports
RUN chown dehub:dehub /var/exports
USER dehub:dehub
ARG JAR_FILE=target/dehub-rest-?.?.?.jar
#ARG JAR_FILE=target/dehub-rest-?.?.?-SNAPSHOT.jar
COPY ${JAR_FILE} /opt/app.jar
ENTRYPOINT ["./start.sh"]
