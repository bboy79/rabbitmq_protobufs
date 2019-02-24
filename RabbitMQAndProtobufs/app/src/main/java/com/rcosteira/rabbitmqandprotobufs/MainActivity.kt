package com.rcosteira.rabbitmqandprotobufs

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.rabbitmq.client.*
import com.rcosteira.rabbitmq_protobufs_android.UserProto
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        const val PROTO_FROM_SERVER = "proto_from_server"
        const val JSON_FROM_SERVER = "json_from_server"
        const val PROTO_FROM_ANDROID = "proto_from_android"
        const val JSON_FROM_ANDROID = "json_from_android"
    }

    lateinit var receivedMessageTextView: TextView
    lateinit var connection: Connection
    lateinit var channel: Channel


    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val handler = CoroutineExceptionHandler { _, throwable ->
        Log.e("Exception", ":$throwable")
    }

    private val incomingMessage: MutableLiveData<String> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        job = SupervisorJob()

        setupMessageSendingButtons()

        setupReceivedMessageTextView()

        launch(handler) {
            withContext(Dispatchers.Default) {
                setupRabbitConnection()
                listenToIncomingMessages()
            }

        }
    }

    private fun setupRabbitConnection() {
        connection = createConnection()
        channel = connection.createChannel()
        channel.queueDeclare(JSON_FROM_SERVER, false, false, false, null)
        channel.queueDeclare(PROTO_FROM_SERVER, false, false, false, null)
        channel.queueDeclare(JSON_FROM_ANDROID, false, false, false, null)
        channel.queueDeclare(PROTO_FROM_ANDROID, false, false, false, null)
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
        val protoButton: MaterialButton = findViewById(R.id.protoButton)
        val jsonButton: MaterialButton = findViewById(R.id.jsonButton)

        protoButton.setOnClickListener {
            sendMessage(PROTO_FROM_ANDROID, ::getProtobufMessage)
        }

        jsonButton.setOnClickListener {
            sendMessage(JSON_FROM_ANDROID, ::getJsonMessage)
        }
    }

    private fun sendMessage(queueName: String, formatHandler: () -> ByteArray) {
        val message = formatHandler()

        launch(handler) {
            withContext(Dispatchers.Default) {
                channel.basicPublish("", queueName, null, message)
            }
        }
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
            .setPicture("resources/New Suit.jpeg")
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
                "\"picture\": \"resources/New Suit.jpeg\"," +
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
        channel.basicConsume(PROTO_FROM_SERVER, true, protoConsumer)
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
                    Log.d("MainActivity", "Got a protobuf")
                    val user = UserProto.User.parseFrom(it)
                    incomingMessage.postValue(user.toString())
                }
            }
        }
    }

    private fun listenToJsonQueue() {
        val jsonConsumer = getJsonConsumer()
        channel.basicConsume(JSON_FROM_SERVER, true, jsonConsumer)
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
                    Log.d("MainActivity", "Got a json")
                    incomingMessage.postValue(String(it, charset("UTF-8")))
                }
            }
        }
    }

    override fun onDestroy() {

        connection.close()
        channel.close()

        coroutineContext.cancelChildren()

        super.onDestroy()
    }
}