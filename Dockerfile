FROM openjdk:17-jdk-slim
WORKDIR /app

COPY build/libs/*SNAPSHOT.jar app.jar

# 추가
RUN mkdir /app/settings

CMD ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
