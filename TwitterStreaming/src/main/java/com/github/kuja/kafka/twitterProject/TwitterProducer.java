package com.github.kuja.kafka.twitterProject;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {

    private Logger logger = LoggerFactory.getLogger(TwitterProducer.class);
    private String consumerKey = "ZMTrgRE4b82h1CfGoynDzq3zo";
    private String consumerSecret = "IiLmaiO24qcviyd9WMaEP39A09p0xaOJexHGiVFEkywArBBWvz";
    private String tokenKey = "1333379476944605184-x39U62uRqaY0IMhjubJmwwKLuyaGB6";
    private String tokenSecret = "XAskeg1HcKEAXoEcTORGSFKPs05LmQjF3wzJthmKtP1Du";

    public TwitterProducer(){}

    public static void main(String[] args) {
        new TwitterProducer().run();
    }
    public void run(){
        //create twitter client
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);
        Client client = createTwitterClient(msgQueue);
        client.connect();

        //create kafka producer
        KafkaProducer<String,String> producer = createKafkaProducer();

        //add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logger.info("stopping application");
            logger.info("shutting down client from twitter");
            client.stop();
            logger.info("closing producer");
            //closing producer ensures that all the remaining msg in memory to kafka before closing
            producer.close();
            logger.info("done");
        }));
        //loop to send data to kafka
        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }
            if(msg!=null){
                logger.info(msg);
                producer.send(new ProducerRecord<>("twitter-tweets", null, msg), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if(e!=null){
                            logger.error("Something bad happened",e);
                        }
                    }
                });
            }

        }
        logger.info("Application is exiting");
    }
    //Twitter client using hbc core - https://github.com/twitter/hbc
    public Client createTwitterClient(BlockingQueue<String> msgQueue){

        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        List<String> terms = Lists.newArrayList("bitcoin","usa","cricket","elections");
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey,consumerSecret,tokenKey,tokenSecret);
        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));


        Client hosebirdClient = builder.build();
        // Attempts to establish a connection.
        return hosebirdClient;


    }

    public KafkaProducer<String,String> createKafkaProducer(){
        String bootstrapServers = "127.0.0.1:9092";

        //set producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());

        //safe producer
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG,"all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG,Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"5");

        //producer with high throughput (this will introduce some latency and CPU usage)
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,Integer.toString(32*1024));


        //create producer
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(properties);
        return producer;
    }
}
