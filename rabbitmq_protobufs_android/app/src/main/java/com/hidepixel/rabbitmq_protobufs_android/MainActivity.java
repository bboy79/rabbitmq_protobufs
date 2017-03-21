package com.hidepixel.rabbitmq_protobufs_android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private static final String PROTO_QUEUE_NAME = "proto";
    private static final String JSON_QUEUE_NAME = "json";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button button = (Button) findViewById(R.id.button);
        Button button2 = (Button) findViewById(R.id.button2);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getProtobufMessageFromRabbit();
                            getJSONMessageFromRabbit();
                        } catch (IOException | TimeoutException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sendProtobufMessageToRabbit();
                            sendJSONMessageToRabbit();
                        } catch (IOException | InterruptedException | TimeoutException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    public void sendJSONMessageToRabbit() throws IOException, TimeoutException, InterruptedException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("10.0.2.2");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(JSON_QUEUE_NAME, false, false, false, null);


        String json = "id: '56fa595034b950a97a63e3a0', " +
                "updated_at: 1459247440, " +
                "created_at: 1459247440, " +
                "web_site: 'http://datsite.com/', " +
                "description: 'Gotta love some description', " +
                "name: 'Say my name', " +
                "version: 0, " +
                "picture: 'applications/56fa595034b950a97a63e3a0/086eda82-3d08-4b4d-ba38-2596d0bf6a98.jpeg'";

        channel.basicPublish("", JSON_QUEUE_NAME, null, json.getBytes());

        channel.close();
        connection.close();
    }

    public void sendProtobufMessageToRabbit() throws IOException, TimeoutException, InterruptedException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("10.0.2.2"); // localhost equivalent for the android emulator
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(PROTO_QUEUE_NAME, false, false, false, null);

        PartnerProto.Partner message = PartnerProto.Partner.newBuilder()
                .setId("56fa595034b950a97a63e3a0")
                .setUpdatedAt(1459247440)
                .setCreatedAt(1459247440)
                .setWebSite("http://datsite.com/")
                .setDescription("Gotta love some description")
                .setName("Say my name")
                .setVersion(0)
                .setPicture("applications/56fa595034b950a97a63e3a0/086eda82-3d08-4b4d-ba38-2596d0bf6a98.jpeg")
                .build();

        channel.basicPublish("", PROTO_QUEUE_NAME, null, message.toByteArray());

        channel.close();
        connection.close();
    }

    public void getProtobufMessageFromRabbit() throws IOException, TimeoutException {

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("10.0.2.2");
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(PROTO_QUEUE_NAME, false, false, false, null);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                PartnerProto.Partner proto = PartnerProto.Partner.parseFrom(body);


                Log.v("rabbitmq receive", "Received protobuf '" + proto.toString() + "'");
            }
        };

        channel.basicConsume(PROTO_QUEUE_NAME, true, consumer);
    }

    public void getJSONMessageFromRabbit() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("10.0.2.2");
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(JSON_QUEUE_NAME, false, false, false, null);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String jsonMessage = new String(body, "UTF-8");

                Log.v("rabbitmq receive", "Received json '" + jsonMessage + "'");
            }
        };

        channel.basicConsume(JSON_QUEUE_NAME, true, consumer);
    }

}
