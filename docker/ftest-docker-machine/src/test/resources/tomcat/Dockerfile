FROM tutum/tomcat:7.0


ENV JAVA_OPTS -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
ADD tomcat-users.xml /tomcat/conf/
EXPOSE 8089
CMD ["/tomcat/bin/catalina.sh","run"]