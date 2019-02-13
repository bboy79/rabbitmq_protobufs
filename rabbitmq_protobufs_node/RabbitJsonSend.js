#!/usr/bin/env node

const amqp = require("amqplib/callback_api");

const sendData = (error, channel) => {
  const q = "json";

  const json = JSON.stringify({
    id: "56fa595034b950a97a63e3a0",
    updated_at: 1459247440,
    created_at: 1459247440,
    name: "Barry Allen",
    version: 1,
    picture: "resources/notANude.jpeg",
    phones: [
      {
        number: "+351987654321",
        type: 0
      },
      {
        number: "+351123456789",
        type: 2
      }
    ]
  });

  channel.assertQueue(q, { durable: false });
  // Note: on Node 6 Buffer.from(msg) should be used
  channel.sendToQueue(q, new Buffer(json));
};

amqp.connect("amqp://localhost", (error, connection) => {
  connection.createChannel(sendData);

  setTimeout(() => {
    connection.close();
    process.exit(0);
  }, 500);
});
