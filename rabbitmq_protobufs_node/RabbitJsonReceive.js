#!/usr/bin/env node

var amqp = require('amqplib/callback_api');

amqp.connect('amqp://localhost', function(err, conn) {
  conn.createChannel(function(err, ch) {
    var q = 'json';

    ch.assertQueue(q, {durable: false});
    console.log(" [*] Waiting for messages in %s. To exit press CTRL+C", q);

    ch.consume(q, function(msg) {
      var json = msg.content.toString();

      console.log(json);

    }, {noAck: true});
  });
});
