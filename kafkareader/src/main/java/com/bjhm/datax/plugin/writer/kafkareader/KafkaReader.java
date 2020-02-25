package com.bjhm.datax.plugin.writer.kafkareader;

import com.alibaba.datax.common.element.*;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;


public class KafkaReader extends Reader {

    public static class Job extends Reader.Job {

        private static final Logger logger = LoggerFactory.getLogger(Job.class);
        private Configuration conf = null;

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            List<Configuration> configurations = new ArrayList<Configuration>(mandatoryNumber);
            for (int i = 0; i < mandatoryNumber; i++) {
                configurations.add(conf);
            }
            return configurations;
        }

        private void validateParameter() {
            this.conf.getNecessaryValue(Key.BOOTSTRAP_SERVERS, KafkaReaderErrorCode.REQUIRED_VALUE);
            this.conf.getNecessaryValue(Key.TOPIC, KafkaReaderErrorCode.REQUIRED_VALUE);
        }

        @Override
        public void init() {
            this.conf = super.getPluginJobConf();
            logger.info("kafka writer params:{}", conf.toJSON());
            this.validateParameter();
        }


        @Override
        public void destroy() {

        }
    }

    public static class Task extends Reader.Task {
        private static final Logger logger = LoggerFactory.getLogger(Task.class);

        private KafkaConsumer<String, String> consumer;
        private Configuration conf;
        private Properties props;

        @Override
        public void init() {
            this.conf = super.getPluginJobConf();

            props = new Properties();
            props.put("bootstrap.servers", conf.getString(Key.BOOTSTRAP_SERVERS));
            props.put("group.id", conf.getString(Key.GROUP_ID) != null ? conf.getString(Key.GROUP_ID)  : UUID.randomUUID().toString());
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("enable.auto.commit", "false");
            consumer = new KafkaConsumer<String, String>(props);
            consumer.subscribe(Arrays.asList(conf.getString(Key.TOPIC)));
        }


        @Override
        public void startRead(RecordSender recordSender) {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    logger.info("offset = "+record.offset()+"---"+ "key = "+record.key()+"----"+ "value ="+ record.value());
                    Record record1 = recordSender.createRecord();
                    String[] splits = record.value().split(" ");
//                    record1.addColumn(new LongColumn(record.offset()));
//                    record1.addColumn(new StringColumn(record.key()));
                    for (String split : splits) {
                        record1.addColumn(new StringColumn(split));
                    }
                    recordSender.sendToWriter(record1);
                    logger.info("缓存记录------------"+record1.toString());
                    consumer.commitSync();
                }
                recordSender.flush();
            }
        }
        @Override
        public void destroy() {
            if (consumer != null) {
                consumer.close();
            }
        }

    }
}
