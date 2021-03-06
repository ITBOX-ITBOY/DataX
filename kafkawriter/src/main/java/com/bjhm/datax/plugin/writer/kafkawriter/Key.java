package com.bjhm.datax.plugin.writer.kafkawriter;

public class Key {

    // must have
    public static final String BOOTSTRAP_SERVERS="bootstrapServers";

    public static final String TOPIC = "topic";

    public static final String ACK = "ack";

    public static final String BATCH_SIZE = "batchSize";

    public static final String RETRIES = "retries";

    public static final String KEYSERIALIZER = "keySerializer";

    public static final String VALUESERIALIZER = "valueSerializer";


    // not must , not default
    public static final String FIELD_DELIMITER = "fieldDelimiter";

    public static final String TOPIC_NUM_PARTITION = "topicNumPartition";

    public static final String TOPIC_REPLICATION_FACTOR = "topicReplicationFactor";

    public static final String WRITE_TYPE= "writeType";
}
