package com.bjhm.datax.plugin.writer.kafkareader.KafkaReader;

public enum ReaderType {
    JSON("json"),
    TEXT("text");

    private String name;

    ReaderType(String name) {
        this.name = name;
    }
}
