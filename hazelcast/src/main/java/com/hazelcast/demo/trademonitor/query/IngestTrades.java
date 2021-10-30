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

package com.hazelcast.demo.trademonitor.query;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.demo.trademonitor.application.ApplicationConfig;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.banking.trademonitor.MyConstants;

import java.util.Map.Entry;
import java.util.Properties;

/**
 * <p>Creates a Jet pipeline to upload from a Kafka topic into a
 * Hazelcast map.
 */
public class IngestTrades {

    private static final long LOG_THRESHOLD = 100_000L;


    public static Pipeline buildPipeline(String bootstrapServers) {

        Properties properties = ApplicationConfig.kafkaSourceProperties(bootstrapServers);

        Pipeline pipeline = Pipeline.create();

        StreamStage<Entry<String, HazelcastJsonValue>> inputSource =
            pipeline.readFrom(KafkaSources.<String, String, Entry<String, HazelcastJsonValue>>
                kafka(properties,
                record -> Util.entry(record.key(), new HazelcastJsonValue(record.value())),
                MyConstants.KAFKA_TOPIC_NAME_TRADES)
                )
         .withoutTimestamps();

        inputSource
         .writeTo(Sinks.map("trades"));

        /* To help with diagnostics, allow every 100,0000th item through
         * on each node. Nulls are filtered out.
         */
        inputSource
        .mapUsingService(ServiceFactories.sharedService(__ -> new LongAccumulator()),
            (counter, item) -> {
                counter.subtract(1);
                if (counter.get() <= 0) {
                    counter.set(LOG_THRESHOLD);
                    return item;
                }
                return null;
        }).setName("filter_every_" + LOG_THRESHOLD)
        .writeTo(Sinks.logger());

        return pipeline;
    }

}
