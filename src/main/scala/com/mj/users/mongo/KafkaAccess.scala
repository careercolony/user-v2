package com.mj.users.mongo

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import com.mj.users.config.Application.{brokers}

trait KafkaAccess {

  def sendPostToKafka(post: String , topic : String): Unit = {
    import java.util.Properties

    val props = new Properties()
    props.put("bootstrap.servers", brokers)

    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)
    val record: ProducerRecord[String, String] = new ProducerRecord[String, String](topic, post)
    producer.send(record)
    producer.close()
  }

}
