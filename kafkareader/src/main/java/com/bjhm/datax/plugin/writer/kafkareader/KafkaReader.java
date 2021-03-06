package com.bjhm.datax.plugin.writer.kafkareader;

import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
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
            List<Configuration> configurations = new ArrayList<Configuration>();
            String s = this.conf.getString(Key.TOPIC_NUM_PARTITION, "1");
            Integer partitions =Integer.valueOf(s);
            for (int i = 0; i < partitions; i++) {
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
        private String fieldDelimiter;

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

            //获取分割符
            fieldDelimiter = conf.getUnnecessaryValue(Key.FIELD_DELIMITER, "\t", null);
        }


        @Override
        public void startRead(RecordSender recordSender) {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    Record record1 = recordSender.createRecord();
                    String[] splits = record.value().split(fieldDelimiter);
                    for (String split : splits) {
                        record1.addColumn(new StringColumn(split));
                    }
                    recordSender.sendToWriter(record1);
                    recordSender.flush();
                    consumer.commitSync();
                }

            }
        }
        @Override
        public void destroy() {
//            if (consumer != null) {
//                consumer.close();
//            }
        }

    }
}
