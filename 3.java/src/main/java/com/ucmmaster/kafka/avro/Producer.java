package com.ucmmaster.kafka.avro;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class Producer {

    private final Logger logger = LoggerFactory.getLogger(Producer.class.getName());

    private KafkaProducer<String, TemperatureRead> producer;

    public Producer(String config) {
        this.producer = createProducer(config);
    }

    private KafkaProducer<String, TemperatureRead> createProducer(String config) {
        // Load properties from the resources folder
        Properties properties = new Properties();
        try (InputStream input = Producer.class.getClassLoader().getResourceAsStream(config)) {
            if (input == null) {
                logger.error("Sorry, unable to find " + config + " in classpath");
                return null;
            }
            properties.load(input);
            return new KafkaProducer<String, TemperatureRead>(properties);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        return null;

    }

    public void produce(String topic) {
        try {
            final Random random = new Random();
            while (true) {
                TimeUnit.SECONDS.sleep(random.longs(0, 5).findFirst().orElse(0));
                com.ucmmaster.kafka.simple.TemperatureRead tr = com.ucmmaster.kafka.simple.TemperatureRead.newRandomTemperatureRead();
                String key = String.valueOf(tr.getId());
                TemperatureRead value = new TemperatureRead();
                value.setId(tr.getId());
                value.setTemperature(tr.getTemperature());
                ProducerRecord<String, TemperatureRead> record = new ProducerRecord<>(topic, key, value);
                record.headers().add("client","java".getBytes(StandardCharsets.UTF_8));
                producer.send(record, (recordMetadata, e) -> {
                    if (e == null) {
                        logger.info("record produced with key:{} value:{} partition:{} offset:{} timestamp:{}",
                                key,
                                value,
                                recordMetadata.partition(),
                                recordMetadata.offset(),
                                recordMetadata.timestamp());
                    } else {
                        logger.error("Error while producing", e);
                    }
                });
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        } finally {
            producer.flush();
            producer.close();
            logger.error("Producer closed.");
        }
    }
}