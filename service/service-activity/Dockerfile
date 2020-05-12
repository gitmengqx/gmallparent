FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY ./target/service-activity.jar service-activity.jar
ENTRYPOINT ["java","-jar","/service-activity.jar", "&"]