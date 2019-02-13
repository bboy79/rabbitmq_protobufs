#!/usr/bin/env node

const amqp = require("amqplib/callback_api");
const protobuf = require("protocol-buffers");
const fs = require("fs");

const userProto = "../rabbitmq_protobufs_android/app/src/main/proto/user.proto";

const consumeData = (error, channel) => {
  const q = "proto";

  const messages = protobuf(fs.readFileSync(userProto));

  channel.assertQueue(q, { durable: false });
  console.log(" [*] Waiting for messages in %s queue.", q);

  channel.consume(
    q,
    msg => {
      const obj = messages.User.decode(msg.content);
      console.log(obj);
    },
    { noAck: true }
  );
};

amqp.connect("amqp://localhost", (error, conn) => {
  conn.createChannel(consumeData);
});
