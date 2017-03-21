#!/usr/bin/env node

var amqp = require('amqplib/callback_api');

amqp.connect('amqp://localhost', function(err, conn) {
  conn.createChannel(function(err, ch) {
    var q = 'json';

    var json = JSON.stringify({
      id: '56fa595034b950a97a63e3a0',
      updated_at: 1459247440,
      created_at: 1459247440,
      web_site: 'http://datsite.com/',
      description: 'Gotta love some description',
      name: 'Say my name',
      version: 0,
      picture: 'applications/56fa595034b950a97a63e3a0/086eda82-3d08-4b4d-ba38-2596d0bf6a98.jpeg'
    });

    ch.assertQueue(q, {durable: false});
    // Note: on Node 6 Buffer.from(msg) should be used
    ch.sendToQueue(q, new Buffer(json));

  });
  setTimeout(function() { conn.close(); process.exit(0) }, 500);
});
