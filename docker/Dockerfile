FROM openjdk:8-jdk

ENV AXONSERVER_HOME /opt/axonserver
RUN mkdir -p ${AXONSERVER_HOME}

COPY axonserver-4.0-exec.jar ${AXONSERVER_HOME}/axonserver.jar
COPY docker/axonserver.properties ${AXONSERVER_HOME}/axonserver.properties
COPY axonserver-cli-4.0-exec.jar ${AXONSERVER_HOME}/axonserver-cli.jar
COPY docker/startup.sh ${AXONSERVER_HOME}/startup.sh
RUN chmod 755 ${AXONSERVER_HOME}/startup.sh

WORKDIR ${AXONSERVER_HOME}
CMD [ "/bin/sh", "-c", "${AXONSERVER_HOME}/startup.sh" ]