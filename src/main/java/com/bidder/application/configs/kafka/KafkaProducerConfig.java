/* (C) 2026
bidder.app */
package com.bidder.application.configs.kafka;

import com.bidder.application.configs.FeatureFlags;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

/**
 * Shared producer for identity, bidding and catalog events. Identical to the KafkaProducerConfig
 * each of those services defined on its own.
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class KafkaProducerConfig {

  private final FeatureFlags featureFlags;

  @Bean
  public ProducerFactory<String, Object> producerFactory(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
      KafkaClientProperties kafkaClientProperties) {
    var configProperties = new HashMap<String, Object>(kafkaClientProperties.properties());
    configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    configProperties.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
    return new DefaultKafkaProducerFactory<>(configProperties);
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory) {
      @Override
      public CompletableFuture<SendResult<String, Object>> send(String topic, Object data) {
        if (!featureFlags.sendNotifications()) {
          log.info("send-notifications feature flag is off, skipping message to topic {}", topic);
          return CompletableFuture.completedFuture(null);
        }
        return super.send(topic, data);
      }
    };
  }
}
