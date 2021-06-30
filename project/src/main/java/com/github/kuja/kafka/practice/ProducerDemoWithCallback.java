package com.github.kuja.kafka.practice;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoWithCallback {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class);

        String bootstrapServers = "127.0.0.1:9092";

        //set producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());

        //create producer
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(properties);

        for(int i=0;i<5;i++) {
            //create record
            ProducerRecord<String, String> record = new ProducerRecord<>("first_topic", "hello world"+Integer.toString(i+1));

            //send data - this is asynchronous
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    //this func is called when the record is produces successfully or there is some error
                    if (e == null) {
                        logger.info("Received new metadata! \n" +
                                "Topic: " + recordMetadata.topic() +
                                "\n Partition: " + recordMetadata.partition() +
                                "\n Offset: " + recordMetadata.offset() +
                                "\n Timestamp: " + recordMetadata.timestamp());
                    } else {
                        logger.error("Error while producing " + e);
                    }

                }
            });
        }
        /*flush data - to wait for data to be produced before ending program
        * otherwise send data will be in a separate background task and program will end before actually sending
        * data*/
        producer.flush();
        //close producer
        producer.close();


    }
}
