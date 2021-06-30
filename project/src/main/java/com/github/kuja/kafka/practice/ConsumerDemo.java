package com.github.kuja.kafka.practice;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

public class ConsumerDemo {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(ConsumerDemo.class);
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my_fourth_application";
        String topic = "first_topic";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        //producer sends data in bytes so consumer needs to convert bytes back to string - called deserialization
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId);
        //latest - only new messages
        //earliest - all messages from beginning
        //none - throws error if no offset is being saved
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        //create consumer
        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);

        //subscribe to a single topic
        consumer.subscribe(Collections.singleton(topic));
        // subscribing to multiple topics
        //consumer.subscribe(Arrays.asList("first_topic","second_topic"));

        //poll for new data - consumer doesn't get data until it asks for it
        while(true){
            ConsumerRecords<String,String> records = consumer.poll(Duration.ofMillis(100));
            for(ConsumerRecord<String,String> record : records){
                logger.info("Key: " + record.key() + ", Value: "+ record.value());
                logger.info("Partition: " + record.partition() + ", Offset: "+ record.offset());
            }

        }



    }


}
