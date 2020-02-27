package com.bjhm.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * kafka写数据的插件
 */
public class KafkaWriter extends Writer {

    /**
     * Job部分，作业部分
     */
    public static class Job extends Writer.Job {
        //日志框架
        private static final Logger logger = LoggerFactory.getLogger(Job.class);
        //配置对象
        private Configuration conf = null;

        /**
         * 框架初始化
         */
        @Override
        public void init() {
            //获取用户的自定义配置信息
            this.conf = super.getPluginJobConf();
            logger.info("kafka writer params:{}", conf.toJSON());
            this.validateParameter();
        }

        /**
         * 验证参数是否正确
         */
        private void validateParameter() {
            //验证服务地址列表信息
            this.conf.getNecessaryValue(Key.BOOTSTRAP_SERVERS, KafkaWriterErrorCode.REQUIRED_VALUE);
            //获取并判断topic是否为空
            this.conf.getNecessaryValue(Key.TOPIC, KafkaWriterErrorCode.REQUIRED_VALUE);
        }

        /**
         * 对作业进行切分
         *
         * @param mandatoryNumber 为了做到Reader、Writer任务数对等，这里要求Writer插件必须按照源端的切分数进行切分。否则框架报错！
         * @return
         */

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            List<Configuration> configurations = new ArrayList<Configuration>(mandatoryNumber);
            //获取分区数量，按分区进行切分
            Integer partitions = conf.getInt(this.conf.getUnnecessaryValue(Key.TOPIC_NUM_PARTITION, "1", null));
            for (int i = 0; i < partitions; i++) {
                configurations.add(conf);
            }
            return configurations;
        }

        @Override
        public void destroy() {

        }
    }

    /*
        Task部分，任务
     */
    public static class Task extends Writer.Task {
        private static final Logger logger = LoggerFactory.getLogger(Task.class);
        private static final String NEWLINE_FLAG = System.getProperty("line.separator", "\n");

        private Producer<String, String> producer;
        private String fieldDelimiter;
        private Configuration conf;
        private Properties props;

        @Override
        public void init() {
            //获取自定义配置信息
            this.conf = super.getPluginJobConf();
            //获取分割符
            fieldDelimiter = conf.getUnnecessaryValue(Key.FIELD_DELIMITER, "\t", null);
            //kafka参数初始化
            kafkaSetParams();
        }

        /**
         * kafka参数配置
         */
        private void kafkaSetParams() {
            props = new Properties();
            props.put("bootstrap.servers", conf.getString(Key.BOOTSTRAP_SERVERS));
            props.put("acks", conf.getUnnecessaryValue(Key.ACK, "0", null));//这意味着leader需要等待所有备份都成功写入日志，这种策略会保证只要有一个备份存活就不会丢失数据。这是最强的保证。
            props.put("retries", conf.getUnnecessaryValue(Key.RETRIES, "0", null));
            // Controls how much bytes sender would wait to batch up before publishing to Kafka.
            //控制发送者在发布到kafka之前等待批处理的字节数。
            //控制发送者在发布到kafka之前等待批处理的字节数。 满足batch.size和ling.ms之一，producer便开始发送消息
            //默认16384   16kb
            props.put("batch.size", conf.getUnnecessaryValue(Key.BATCH_SIZE, "16384", null));
            props.put("linger.ms", 1);
            props.put("key.serializer", conf.getUnnecessaryValue(Key.KEYSERIALIZER, "org.apache.kafka.common.serialization.StringSerializer", null));
            props.put("value.serializer", conf.getUnnecessaryValue(Key.VALUESERIALIZER, "org.apache.kafka.common.serialization.StringSerializer", null));
            producer = new KafkaProducer<String, String>(props);
        }

        @Override
        public void prepare() {
            //获取topic列表
            ListTopicsResult topicsResult = AdminClient.create(props).listTopics();
            //判断topic是否为空
            String topic = conf.getNecessaryValue(Key.TOPIC, KafkaWriterErrorCode.REQUIRED_VALUE);

            try {
                //判断topic列表中是否存配置文件中配置的topic
                if (!topicsResult.names().get().contains(topic)) {
                    /* 如topic不存在就创建一个 */
                    NewTopic newTopic = new NewTopic(
                            topic,
                            Integer.valueOf(conf.getUnnecessaryValue(Key.TOPIC_NUM_PARTITION, "1", null)),
                            Short.valueOf(conf.getUnnecessaryValue(Key.TOPIC_REPLICATION_FACTOR, "1", null))
                    );
                    List<NewTopic> newTopics = new ArrayList<NewTopic>();
                    newTopics.add(newTopic);
                    /* 创建topic */
                    AdminClient.create(props).createTopics(newTopics);
                }
            } catch (Exception e) {
                throw new DataXException(KafkaWriterErrorCode.CREATE_TOPIC, KafkaWriterErrorCode.REQUIRED_VALUE.getDescription());
            }
        }

        /**
         * 开始写数据
         *
         * @param lineReceiver
         */
        @Override
        public void startWrite(RecordReceiver lineReceiver) {
            logger.info("start to writer kafka");
            Record record = null;
            while ((record = lineReceiver.getFromReader()) != null) {//说明还在读取数据,或者读取的数据没处理完
                //获取一行数据，按照指定分隔符 拼成字符串 发送出去
                if (conf.getUnnecessaryValue(Key.WRITE_TYPE, WriteType.TEXT.name(), null).toLowerCase()
                        .equals(WriteType.TEXT.name().toLowerCase())) {
                    logger.info("我走了发送的 text");
                    producer.send(new ProducerRecord<String, String>(this.conf.getString(Key.TOPIC),
                            recordToString(record),
                            recordToString(record))
                    );
                } else if (conf.getUnnecessaryValue(Key.WRITE_TYPE, WriteType.TEXT.name(), null).toLowerCase()
                        .equals(WriteType.JSON.name().toLowerCase())) {
                    logger.info("我走了text的json");
                    producer.send(new ProducerRecord<String, String>(this.conf.getString(Key.TOPIC),
                            recordToString(record),
                            record.toString())
                    );
                }
                logger.info("complete write " + record.toString());
                producer.flush();
                logger.info("end to writer kafka");
            }
        }

        @Override
        public void destroy() {
            if (producer != null) {
                producer.close();
            }
        }

        /**
         * 数据格式化
         *
         * @param record
         * @return
         */
        private String recordToString(Record record) {
            int recordLength = record.getColumnNumber();
            if (0 == recordLength) {
                return NEWLINE_FLAG;
            }
            Column column;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < recordLength; i++) {
                column = record.getColumn(i);
                sb.append(column.asString()).append(fieldDelimiter);
            }

            sb.setLength(sb.length() - 1);
            sb.append(NEWLINE_FLAG);

            return sb.toString();
        }
    }
}
