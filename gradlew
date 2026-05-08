#!/bin/sh
# Gradle wrapper script
APP_HOME=$(cd "$(dirname "$0")" && pwd)
exec "${JAVA_HOME:-/usr}/bin/java" $JAVA_OPTS \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"
