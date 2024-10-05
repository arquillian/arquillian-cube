FROM tomcat:10.1.30

ENV JAVA_OPTS -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
ADD tomcat-users.xml conf/
EXPOSE 8089
CMD ["catalina.sh","run"]
