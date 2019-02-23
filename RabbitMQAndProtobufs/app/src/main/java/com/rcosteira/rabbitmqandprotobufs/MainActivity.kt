package com.rcosteira.rabbitmqandprotobufs

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.rabbitmq.client.*
import com.rcosteira.rabbitmq_protobufs_android.UserProto

class MainActivity : AppCompatActivity() {

    companion object {
        const val PROTO_Q_NAME = "proto"
        const val JSON_Q_NAME = "json"
    }

    lateinit var protoButton: MaterialButton
    lateinit var jsonButton: MaterialButton
    lateinit var receivedMessageTextView: TextView
    lateinit var connection: Connection
    lateinit var channel: Channel

    private val incomingMessage: MutableLiveData<String> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRabbitConnection()

        setupMessageSendingButtons()

        setupReceivedMessageTextView()

        listenToIncomingMessages()
    }

    private fun setupRabbitConnection() {
        connection = createConnection()
        channel = connection.createChannel()

        channel.queueDeclare(JSON_Q_NAME, false, false, false, null)
        channel.queueDeclare(PROTO_Q_NAME, false, false, false, null)
    }

    private fun createConnection(): Connection {
        val connectionFactory = getConnectionFactory()
        return connectionFactory.newConnection()
    }

    private fun getConnectionFactory(): ConnectionFactory {
        val connectionFactory = ConnectionFactory()
        connectionFactory.host = "10.0.2.2" // localhost equivalent for the android emulator

        return connectionFactory
    }

    private fun setupMessageSendingButtons() {
        protoButton = findViewById(R.id.protoButton)
        jsonButton = findViewById(R.id.jsonButton)

        protoButton.setOnClickListener {
            sendMessage(PROTO_Q_NAME, ::getProtobufMessage)
        }

        jsonButton.setOnClickListener {
            sendMessage(JSON_Q_NAME, ::getJsonMessage)
        }
    }

    private fun sendMessage(queueName: String, formatHandler: () -> ByteArray) {
        val message = formatHandler()

        channel.basicPublish("", queueName, null, message)
    }


    private fun getProtobufMessage(): ByteArray {
        val phones = listOf(
            UserProto.User.PhoneNumber.newBuilder()
                .setNumber("+123456789")
                .setType(UserProto.User.PhoneType.HOME)
                .build(),
            UserProto.User.PhoneNumber.newBuilder()
                .setNumber("+987654321")
                .setType(UserProto.User.PhoneType.MOBILE)
                .build()
        )

        val user = UserProto.User.newBuilder()
            .setId("56fa595034b950a97a63e3a0")
            .setUpdatedAt(System.currentTimeMillis())
            .setCreatedAt(System.currentTimeMillis())
            .setName("Barry Allen")
            .setVersion(1)
            .setPicture("applications/56fa595034b950a97a63e3a0/086eda82-3d08-4b4d-ba38-2596d0bf6a98.jpeg")
            .addAllPhones(phones)
            .build()

        return user.toByteArray()
    }

    private fun getJsonMessage(): ByteArray {
        val json = "{" +
                "\"User\": {" +
                "\"id\": 56fa595034b950a97a63e3a0," +
                "\"updated_at\": \"${System.currentTimeMillis()}\", " +
                "\"created_at\": \"${System.currentTimeMillis()}\", " +
                "\"name\": \"Barry Allen\"," +
                "\"version\": \"1," +
                "\"picture\": \"applications/56fa595034b950a97a63e3a0/086eda82-3d08-4b4d-ba38-2596d0bf6a98.jpeg\"," +
                "\"phones\": [" +
                "{" +
                "\"number\": \"+123456789\"," +
                "\"type\": \"HOME\"" +
                "}," +
                "{" +
                "\"number\": \"+987654321\"," +
                "\"type\": \"MOBILE\"" +
                "}" +
                "]" +
                "}" +
                "}"

        return json.toByteArray()
    }

    private fun setupReceivedMessageTextView() {
        receivedMessageTextView = findViewById(R.id.receivedMessageTextView)
        incomingMessage.observe(this, Observer {
            receivedMessageTextView.text = it
        })
    }

    private fun listenToIncomingMessages() {
        listenToProtoQueue()
        listenToJsonQueue()
    }

    private fun listenToProtoQueue() {
        val protoConsumer = getProtoConsumer()
        channel.basicConsume(PROTO_Q_NAME, true, protoConsumer)
    }

    private fun getProtoConsumer(): Consumer {
        return object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope?,
                properties: AMQP.BasicProperties?,
                body: ByteArray?
            ) {
                body?.let {
                    val user = UserProto.User.parseFrom(it)
                    incomingMessage.postValue(user.toString())
                }
            }
        }
    }

    private fun listenToJsonQueue() {
        val jsonConsumer = getJsonConsumer()
        channel.basicConsume(PROTO_Q_NAME, true, jsonConsumer)
    }

    private fun getJsonConsumer(): Consumer {
        return object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope?,
                properties: AMQP.BasicProperties?,
                body: ByteArray?
            ) {
                body?.let {
                    incomingMessage.postValue(String(it))
                }
            }
        }
    }

    override fun onDestroy() {

        connection.close()
        channel.close()

        super.onDestroy()
    }
}

/**
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
 */