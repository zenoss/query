#!/bin/sh

JVM_ARGS="$JVM_ARGS"
JVM_XMX="-Xmx1024m"

exec java -server -XX:+HeapDumpOnOutOfMemoryError ${JVM_XMX} ${JVM_ARGS}  -jar lib/${project.artifactId}/${project.artifactId}-${project.version}.jar server etc/${project.artifactId}/configuration.yaml
