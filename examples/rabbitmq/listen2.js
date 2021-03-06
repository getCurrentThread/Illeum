#!/usr/bin/env node

var amqp = require('amqplib/callback_api');
// import amqp from('amqplib/callback_api');

amqp.connect('amqp://illeum:illeum123@k4d106.p.ssafy.io:5672', function(error0, connection) {
    if (error0) {
        throw error0;
    }
    connection.createChannel(function(error1, channel) {
        if (error1) {
            throw error1;
        }

        var queue = 'member.12';

        channel.assertQueue(queue, {
            durable: true
        });

        console.log(" [*] Waiting for messages in %s. To exit press CTRL+C", queue);

        channel.consume(queue, function(msg) {
            console.log(" [x] Received %s", msg.content.toString());
        }, {
            noAck: true
        });
    });
});