#!/usr/bin/env node

const amqp = require("amqplib/callback_api");

const consumeData = (error, channel) => {
  const q = "json";

  channel.assertQueue(q, { durable: false });
  console.log(" [*] Waiting for messages in %s queue.", q);

  channel.consume(
    q,
    msg => {
      const json = msg.content.toString();

      console.log(json);
    },
    { noAck: true }
  );
};

amqp.connect("amqp://localhost", (error, connection) => {
  connection.createChannel(consumeData);
});
