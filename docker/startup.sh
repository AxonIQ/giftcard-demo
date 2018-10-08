#!/bin/bash

export AXONSERVER_HOME=/opt/axonserver
cd ${AXONSERVER_HOME}

echo >> ${AXONSERVER_HOME}/axonserver.properties

# can be provided through Docker/Kubernetes:
# - AXONSERVER_NAME
if [ "x${AXONSERVER_NAME}" != "x" ] ; then
  echo "axoniq.axonserver.name=${AXONSERVER_NAME}" >> ${AXONSERVER_HOME}/axonserver.properties
fi
# - AXONSERVER_HOSTNAME
if [ "x${AXONSERVER_HOSTNAME}" != "x" ] ; then
  echo "axoniq.axonserver.hostname=${AXONSERVER_HOSTNAME}" >> ${AXONSERVER_HOME}/axonserver.properties
fi
# - AXONSERVER_DOMAIN
if [ "x${AXONSERVER_DOMAIN}" != "x" ] ; then
  echo "axoniq.axonserver.domain=${AXONSERVER_DOMAIN}" >> ${AXONSERVER_HOME}/axonserver.properties
fi
# - AXONSERVER_HTTP_PORT
if [ "x${AXONSERVER_HTTP_PORT}" != "x" ] ; then
  echo "server.port=${AXONSERVER_HTTP_PORT}" >> ${AXONSERVER_HOME}/axonserver.properties
fi
# - AXONSERVER_GRPC_PORT
if [ "x${AXONSERVER_GRPC_PORT}" != "x" ] ; then
  echo "axoniq.axonserver.port=${AXONSERVER_GRPC_PORT}" >> ${AXONSERVER_HOME}/axonserver.properties
fi
# - AXONSERVER_TOKEN
if [ "x${AXONSERVER_TOKEN}" != "x" ] ; then
  echo "axoniq.axonserver.accesscontrol.enabled=true" >> ${AXONSERVER_HOME}/axonserver.properties
  echo "axoniq.axonserver.accesscontrol.token=${AXONSERVER_TOKEN}" >> ${AXONSERVER_HOME}/axonserver.properties
else
  echo "axoniq.axonserver.accesscontrol.enabled=false" >> ${AXONSERVER_HOME}/axonserver.properties
  echo "axoniq.axonserver.accesscontrol.token=" >> ${AXONSERVER_HOME}/axonserver.properties
fi

java -Djava.security.egd=file:/dev/./urandom -Xmx512m -jar ${AXONSERVER_HOME}/axonserver.jar