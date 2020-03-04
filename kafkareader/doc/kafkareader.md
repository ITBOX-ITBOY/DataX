# DataX KafkaReader 插件文档


------------

## 1 快速介绍

KafkaReader提供向kafka中指定topic读数据。


## 2 功能与限制
目前kafkaReader支持向单个topic中写入文本类型数据

## 3 功能说明
目前只支持将kafka数据写入到mysql数据库中

### 3.1 配置样例

```json
{
  "job": {
    "content": [
      {
        "reader": {
          "name": "kafkareader",
          "parameter": {
            "bootstrapServers": "192.168.1.110:9092",
            "fieldFelimiter": "\t",
            "retries": 0,
            "topic": "test"
          }
        },
        "writer": {
          "name": "mysqlwriter",
          "parameter": {
            "column": ["id","name"],
            "password": "123456",
            "username": "root",
            "writeMode": "insert",
            "batchSize": 10,
            "connection": [
              {
                "jdbcUrl": "jdbc:mysql://localhost:3306/datax",
                "table": ["student2"]
              }
            ]
          }
        }
      }
    ],
    "setting": {
      "speed": {
        "channel": "1"
      }
    }
  }
}

```

### 3.2 参数说明

* **bootstrapServers**

	* 描述：kafka服务地址，格式：host1:port,host2:port 样例：10.1.20.111:9092,10.1.20.121:9092<br />

	* 必选：是 <br />

	* 默认值：无 <br />

* **topic**

	* 描述：kafka Topic 名称， 目前支持一次写入单个topic <br />

	* 必选：是 <br />

	* 默认值：无 <br />

* **fieldDelimiter**

	* 描述：当wirteType为text时，写入时的字段分隔符<br />

	* 必选：否 <br />

	* 默认值："\t"


* **keySerializer**

	* 描述：键序列化，默认org.apache.kafka.common.serialization.StringSerializer<br />，目前不支持其他类型

	* 必选：否 <br />

 	* 默认值：org.apache.kafka.common.serialization.StringSerializer <br />

* **valueSerializer**

	* 描述：键序列化，默认org.apache.kafka.common.serialization.StringSerializer<br />,目前不支持其他类型

	* 必选：否 <br />

 	* 默认值：org.apache.kafka.common.serialization.StringSerializer <br />

* **topicNumPartition**

	* 描述：topic Partition 数量<br />

 	* 必选：否 <br />
 
 	* 默认值：1 <br />

### 3.3 类型转换

## 4 配置步骤

## 5 约束限制

略

## 6 FAQ

略
