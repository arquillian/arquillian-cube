FROM java:7

WORKDIR /usr/src/server
COPY ${deployableFilename} /usr/src/server/${deployableFilename}
EXPOSE 8080
CMD ["java", "-jar", "${deployableFilename}"]