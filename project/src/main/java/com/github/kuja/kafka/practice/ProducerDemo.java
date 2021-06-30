package com.github.kuja.kafka.practice;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProducerDemo {
    public static void main(String[] args) {

        String bootstrapServers = "127.0.0.1:9092";

        //set producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());

        //create producer
        //<String,String> means we want the topic nd value to be String
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(properties);

        //create record
        ProducerRecord<String , String> record = new ProducerRecord<>("first_topic","hello world");

        //send data - this is asynchronous
        producer.send(record);

        /*flush data - to wait for data to be produced before ending program
        * otherwise send data will be in a separate background task and program will end before actually sending
        * data*/
        producer.flush();
        //close producer
        producer.close();


    }
}
