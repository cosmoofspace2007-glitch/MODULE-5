
FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

ENV GRADLE_USER_HOME=/tmp/gradle-cache

COPY . .

RUN chmod +x gradlew

ARG MODULE

RUN ./gradlew --no-daemon ":${MODULE}:bootJar" -x test

RUN find "/workspace/${MODULE}/build/libs" \
    -maxdepth 1 \
    -type f \
    -name "*.jar" \
    ! -name "*-plain.jar" \
    -exec cp {} /workspace/app.jar \; \
    && test -f /workspace/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


