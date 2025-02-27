FROM gradle:7.5.1-jdk17-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project
RUN gradle build -x test

FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y wget netcat
RUN wget -O /usr/local/bin/wait-for-it https://github.com/vishnubob/wait-for-it/raw/master/wait-for-it.sh \
    && chmod +x /usr/local/bin/wait-for-it
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

CMD ["wait-for-it", "inventory-db:5432" , "--", "java", "-jar", "app.jar"]


