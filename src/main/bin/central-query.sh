#!/bin/sh

JVM_ARGS="$JVM_ARGS"
JVM_XMX="-Xmx1024m"

LIBDIR=lib/${project.artifactId}
ZAPP_JAR=${LIBDIR}/${project.artifactId}-${project.version}.jar

if [ -f ${ZAPP_JAR} ]; then
    exec java -server -XX:+HeapDumpOnOutOfMemoryError ${JVM_XMX} ${JVM_ARGS}  -jar ${ZAPP_JAR} server etc/${project.artifactId}/configuration.yaml
else
    cd ${LIBDIR} &&  mvn -DskipTests=true compile exec:java
fi
