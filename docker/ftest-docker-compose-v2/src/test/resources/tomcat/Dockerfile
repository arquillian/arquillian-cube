FROM tomcat:10.1.30
RUN cp -avT webapps.dist/manager webapps/manager
ADD context.xml webapps/manager/META-INF/
ADD tomcat-users.xml conf/

CMD ["catalina.sh","run"]
