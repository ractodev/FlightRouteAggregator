FROM openjdk:11 AS build
ENV SBT_VERSION 1.7.3
RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d opt
WORKDIR /App
COPY . /App
# RUN /opt/sbt/bin/sbt clean compile package
RUN /opt/sbt/bin/sbt clean compile assembly

FROM docker.io/bitnami/spark:latest 
COPY --from=build /App/target/scala-2.12 /App
WORKDIR /App
# CMD ["spark-submit", "--class", "Main", "--packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.2.2,org.mongodb.spark:mongo-spark-connector:10.0.0", "flight-route-aggregator.jar"]
CMD ["spark-submit", "--class", "Main", "--packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.2.2,org.mongodb.spark:mongo-spark-connector:10.0.0", "flight-route-aggregator-v2-assembly-0.1.0-SNAPSHOT.jar"]
