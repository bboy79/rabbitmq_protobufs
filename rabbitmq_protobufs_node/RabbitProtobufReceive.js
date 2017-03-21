#!/usr/bin/env node

var amqp = require('amqplib/callback_api');
var protobuf = require('protocol-buffers');
var fs = require('fs');


amqp.connect('amqp://localhost', function(err, conn) {
  conn.createChannel(function(err, ch) {
    var q = 'proto';

    var messages = protobuf(fs.readFileSync('partner.proto'));

    ch.assertQueue(q, {durable: false});
    console.log(" [*] Waiting for messages in %s. To exit press CTRL+C", q);

    ch.consume(q, function(msg) {

      var obj = messages.Partner.decode(msg.content);
      console.log(obj);

    }, {noAck: true});
  });
});
