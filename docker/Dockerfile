FROM openjdk:8-jre-alpine
ADD *.jar /app.jar
RUN sh -c 'chmod 777 /app.jar'
ADD docker-entrypoint.sh /docker-entrypoint.sh
RUN sh -c 'chmod 777 /docker-entrypoint.sh'
ENV DELAY 0
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]