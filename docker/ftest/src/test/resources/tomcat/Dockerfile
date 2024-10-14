FROM tomcat:10.1.30

ENV JAVA_OPTS -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
RUN cp -avT webapps.dist/manager webapps/manager
ADD context.xml webapps/manager/META-INF/
ADD tomcat-users.xml conf/
EXPOSE 8089
CMD ["catalina.sh","run"]
