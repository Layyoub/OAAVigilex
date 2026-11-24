FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw.cmd mvnw.cmd
RUN apt-get update && apt-get install -y curl unzip
COPY src src
RUN curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip -o /tmp/maven.zip && unzip -q /tmp/maven.zip -d /opt && /opt/apache-maven-3.9.9/bin/mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
