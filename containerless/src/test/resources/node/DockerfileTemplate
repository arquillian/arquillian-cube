FROM node:0.11.14

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ADD ${deployableFilename} /usr/src/app
RUN npm install
EXPOSE 8080

CMD [ "npm", "start" ]