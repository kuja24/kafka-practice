package com.github.kuja.kafka.streams;

import com.google.gson.JsonParser;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class StreamsFilterTweets {
    private static JsonParser jsonParser = new JsonParser();
    public static void main(String[] args) {
        //create properties
        Properties properties = new Properties();
        properties.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
        properties.setProperty(StreamsConfig.APPLICATION_ID_CONFIG,"demo-kafka-streams");
        //the below properties define that out keys and values will be strings and this is how we'll serialize & deserialize
        properties.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        properties.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,Serdes.StringSerde.class.getName());

        //create topology
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        //input topic
       KStream<String,String> inputTopic = streamsBuilder.stream("twitter-tweets");
       KStream<String,String> filteredStream = inputTopic.filter(
               (k,jsonTweet) -> extractUserFollowersInTweet(jsonTweet)>10000
                   //filter for tweets which have user of over 10000 followers
        );
       filteredStream.to("important_tweets");

        //build topology
        KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(),properties);
        kafkaStreams.start();

        //start our streams application
    }
    private static Integer extractUserFollowersInTweet(String jsonString){
        //gson parser
        try {
            return jsonParser.parse(jsonString)
                    .getAsJsonObject()
                    .get("user")
                    .getAsJsonObject()
                    .get("followers_count")
                    .getAsInt();
        }catch (NullPointerException e){
            return  0;
        }

    }
}
