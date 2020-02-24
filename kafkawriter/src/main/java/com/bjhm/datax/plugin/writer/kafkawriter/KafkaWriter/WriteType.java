package com.bjhm.datax.plugin.writer.kafkawriter.KafkaWriter;

public enum WriteType {
    JSON("json"),
    TEXT("text");

    private String name;

    WriteType(String name) {
        this.name = name;
    }
}
