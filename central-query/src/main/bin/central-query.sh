#!/bin/sh

JVM_ARGS="$JVM_ARGS"
JVM_XMX="-Xmx4096m"
JVM_XMS="-Xms1024m"

LIBDIR=lib/${project.artifactId}
ZAPP_JAR=${LIBDIR}/${project.artifactId}-${project.version}.jar

if [ -f ${ZAPP_JAR} ]; then
    exec java -server -XX:OnOutOfMemoryError="kill -9 %p" ${JVM_XMX} ${JVM_XMS} ${JVM_ARGS}  -jar ${ZAPP_JAR} server etc/${project.artifactId}/configuration.yaml
else
    # compile and install projects before running
    cd ${LIBDIR} && \
        mvn -DskipTests=true compile install && \
        exec java -server -XX:+HeapDumpOnOutOfMemoryError ${JVM_XMX} ${JVM_XMS} ${JVM_ARGS} \
            -jar ${project.artifactId}/target/${project.artifactId}-${project.version}.jar \
            server ${project.artifactId}/src/main/etc/configuration.yaml
fi
