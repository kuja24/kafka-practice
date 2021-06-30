package com.github.kuja.kafka.practice;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.metrics.stats.Count;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThreads {
    public static void main(String[] args) {
        new ConsumerDemoWithThreads().run();
        }
    private ConsumerDemoWithThreads(){

    }
    private void run()  {
        Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThreads.class);
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my_sixth_application";
        String topic = "first_topic";
        //latch deals with multiple threads
        CountDownLatch latch = new CountDownLatch(1);
        logger.info("Creating consumer thread");
        Runnable myConsumerRunnable = new ConsumerRunnable(
                bootstrapServers,
                groupId,
                topic,
                latch
        );

        Thread myThread = new Thread(myConsumerRunnable);
        myThread.start();

        //add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread( ()->{
            logger.info("adding shutdown hook");
            ((ConsumerRunnable)myConsumerRunnable).shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Application has exited");
        }

        ));
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Application got interrupted",e);
        } finally {
            logger.info("Application is closing");
        }

    }



public class ConsumerRunnable implements Runnable {
    private CountDownLatch latch;
    private KafkaConsumer<String, String> consumer;
    private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class);

    public ConsumerRunnable(String bootstrapServers, String groupId, String topic, CountDownLatch latch) {
        this.latch = latch;

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<String, String>(properties);
        consumer.subscribe(Collections.singleton(topic));
    }

    @Override
    public void run() {
        //poll for new data - consumer doesn't get data until it asks for it
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    logger.info("Key: " + record.key() + ", Value: " + record.value());
                    logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                }
            }
        } catch (WakeupException e) {
            logger.info("Received shutdown signal");
        } finally {
            consumer.close();
            latch.countDown(); // allow main code to understand that we are done with consumer
        }
    }

    public void shutdown() {
        //this wakeup () method interrupts the consumer.poll()
        //it throws an exception calledWakeUpException
        consumer.wakeup();

    }
}

}
