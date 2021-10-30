/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.demo.trademonitor.application;

import com.hazelcast.config.ClasspathYamlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;


public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);


    public static Config buildConfig() {
        Config config = new ClasspathYamlConfig("hazelcast.yml");

        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getAutoDetectionConfig().setEnabled(false);

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("Kubernetes configuration: service-dns: {}",
                    joinConfig.getKubernetesConfig().getProperty("service-dns"));
        } else {
            joinConfig.getKubernetesConfig().setEnabled(false);

            TcpIpConfig tcpIpConfig = new TcpIpConfig();
            tcpIpConfig.setEnabled(true);
            if (System.getProperty("MY_HAZELCAST_SERVERS", "").length() != 0) {
                tcpIpConfig.setMembers(Arrays.asList(System.getProperty("MY_HAZELCAST_SERVERS").split(",")));
            } else {
                String host = System.getProperty("hazelcast.local.publicAddress", "127.0.0.1");
                host = host.replaceAll("5703", "5701").replaceAll("5702", "5701");
                tcpIpConfig.setMembers(Arrays.asList(host.split(",")));
            }

            joinConfig.setTcpIpConfig(tcpIpConfig);

            LOGGER.info("Non-Kubernetes configuration: member-list: {}", tcpIpConfig.getMembers());
        }

        return config;
    }


    public static Properties kafkaSourceProperties(String bootstrapServers) {

        Properties properties = new Properties();

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());

        return properties;
    }

}
