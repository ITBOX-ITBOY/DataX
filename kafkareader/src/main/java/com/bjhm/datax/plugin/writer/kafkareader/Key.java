package com.bjhm.datax.plugin.writer.kafkareader;


public class Key {

    public static final String BOOTSTRAP_SERVERS="bootstrapServers";

    // must have
    public static final String TOPIC = "topic";
    public static final String GROUP_ID="groupId";
    // not must , not default
    public static final String FIELD_DELIMITER = "fieldDelimiter";

    public static final String TOPIC_NUM_PARTITION = "topicNumPartition";

}
