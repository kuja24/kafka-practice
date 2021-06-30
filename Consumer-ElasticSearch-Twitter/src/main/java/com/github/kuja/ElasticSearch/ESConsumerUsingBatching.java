package com.github.kuja.ElasticSearch;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ESConsumerUsingBatching {

    public static RestHighLevelClient createClient() {

        String hostname = "learning-kafka-1735796821.ap-southeast-2.bonsaisearch.net";
        String username = "qz2r3pc057";
        String password = "lfiyoa3fbh";

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                            HttpAsyncClientBuilder httpClientBuilder) {
                        //to apply these credentials to every http request
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

    public static KafkaConsumer<String,String> createConsumer(String topic){
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "elasticsearch-consumer";

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
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"false");
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,"100");
        //create consumer
        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);

        //subscribe to a single topic
        consumer.subscribe(Collections.singleton(topic));
        return consumer;
    }
    private static JsonParser jsonParser = new JsonParser();
    private static String extractIdFromTweet(String jsonString){
        //gson parser
        return jsonParser.parse(jsonString)
                .getAsJsonObject()
                .get("id_str")
                .getAsString();

    }
    public static void main(String[] args) throws IOException {

        Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class);
        RestHighLevelClient client = createClient();

        KafkaConsumer<String,String> consumer = createConsumer("twitter-tweets");
        while(true){
            ConsumerRecords<String,String> records = consumer.poll(Duration.ofMillis(100));
            Integer recordCount = records.count();
            logger.info("Received " + recordCount + " records");
            BulkRequest bulkRequest = new BulkRequest();
            for(ConsumerRecord<String,String> record : records){

                try {
                    String id = extractIdFromTweet(record.value());
                    IndexRequest indexRequest = new IndexRequest(
                            "twitter",
                            "tweets",
                            id
                    ).source(record.value(), XContentType.JSON);

                    bulkRequest.add(indexRequest);
                }catch (NullPointerException e){
                    logger.warn("skipping bad data " + record.value());
                }

            }
            if(recordCount>0) {
                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                logger.info("Comitting offsets..");
                consumer.commitSync();
                logger.info("Comitted offsets..");
            }


        }

        //client.close();


    }
}
