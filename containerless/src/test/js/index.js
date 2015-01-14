var express = require('express');

var app = express();

app.get('/', function(req, res){
    res.send('Hello from inside a container!');
});

app.listen(8080);
