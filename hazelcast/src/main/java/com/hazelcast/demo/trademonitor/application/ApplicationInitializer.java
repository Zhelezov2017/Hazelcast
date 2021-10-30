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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.demo.trademonitor.query.AggregateQuery;
import com.hazelcast.demo.trademonitor.query.IngestTrades;
import com.hazelcast.demo.trademonitor.trade.MaxVolumeAggregator;
import com.hazelcast.demo.trademonitor.trade.Trade;
import com.hazelcast.demo.trademonitor.utils.*;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.map.IMap;

import com.hazelcast.platform.demos.banking.trademonitor.MyConstants;
import com.hazelcast.platform.demos.banking.trademonitor.MyMembershipListener;
import com.hazelcast.platform.demos.banking.trademonitor.MyUtils;
import com.hazelcast.platform.demos.banking.trademonitor.NasdaqFinancialStatus;
import com.hazelcast.platform.demos.banking.trademonitor.NasdaqMarketCategory;
import com.hazelcast.platform.demos.banking.trademonitor.SymbolInfo;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;


public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);
    // Local constant, never needed outside this class
    private static final String APPLICATION_PROPERTIES_FILE = "application.properties";


    public static void initialise(HazelcastInstance hazelcastInstance, String bootstrapServers) throws Exception {
        createNeededObjects(hazelcastInstance);
        loadNeededData(hazelcastInstance, bootstrapServers);
        defineQueryableObjects(hazelcastInstance, bootstrapServers);
        launchNeededJobs(hazelcastInstance, bootstrapServers);
    }





    static void createNeededObjects(HazelcastInstance hazelcastInstance) {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            hazelcastInstance.getMap(iMapName);
        }
    }



    static void loadNeededData(HazelcastInstance hazelcastInstance, String bootstrapServers) throws Exception {
        IMap<String, String> kafkaConfigMap =
                hazelcastInstance.getMap(MyConstants.IMAP_NAME_KAFKA_CONFIG);
        IMap<String, SymbolInfo> symbolsMap =
                hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);

        if (!kafkaConfigMap.isEmpty()) {
            LOGGER.trace("Skip loading '{}', not empty", kafkaConfigMap.getName());
        } else {
            Properties properties = ApplicationConfig.kafkaSourceProperties(bootstrapServers);

            kafkaConfigMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
            kafkaConfigMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    properties.getProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
            kafkaConfigMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    properties.getProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));

            LOGGER.trace("Loaded {} into '{}'", kafkaConfigMap.size(), kafkaConfigMap.getName());
        }

        if (!symbolsMap.isEmpty()) {
            LOGGER.trace("Skip loading '{}', not empty", symbolsMap.getName());
        } else {
            Map<String, SymbolInfo> localMap =
                    MyUtils.nasdaqListed().entrySet().stream()
                    .collect(Collectors.<Entry<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>,
                            String, SymbolInfo>
                            toUnmodifiableMap(
                            entry -> entry.getKey(),
                            entry -> {
                                SymbolInfo symbolInfo = new SymbolInfo();
                                symbolInfo.setSecurityName(entry.getValue().f0());
                                symbolInfo.setMarketCategory(entry.getValue().f1());
                                symbolInfo.setFinancialStatus(entry.getValue().f2());
                                return symbolInfo;
                            }));

            symbolsMap.putAll(localMap);

            LOGGER.trace("Loaded {} into '{}'", localMap.size(), symbolsMap.getName());
        }
    }


    /**
     * <p>Define Hazelcast maps &amp; Kafka topics for later SQL querying.
     * </p>
     */
    static void defineQueryableObjects(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        defineKafka(hazelcastInstance, bootstrapServers);
        defineIMap(hazelcastInstance);
        defineIMap2(hazelcastInstance);
    }


    /**
     * <p>Define Kafka streams so can be directly used as a
     * querying source by SQL.
     * </p>
     *
     * @param bootstrapServers
     */
    static void defineKafka(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        String definition1 = "CREATE EXTERNAL MAPPING IF NOT EXISTS "
                // Name for our SQL
                + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRADES
                // Name of the remote object
                + " EXTERNAL NAME " + MyConstants.KAFKA_TOPIC_NAME_TRADES
                + " ( "
                + " id             VARCHAR, "
                + " price          BIGINT, "
                + " quantity       BIGINT, "
                + " symbol         VARCHAR, "
                // Timestamp is a reserved word, need to escape. Adjust the mapping name so avoiding clash with IMap
                + " \"timestamp\"  BIGINT "
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";

        define(definition1, hazelcastInstance);
    }


    /**
     * <p>Without this metadata, cannot query an empty
     * {@link IMap}.
     * </p>
     *
     * @param hazelcastInstance
     */
    static void defineIMap(HazelcastInstance hazelcastInstance) {
        String definition1 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Tuple3.class.getCanonicalName() + "'"
                + " )";

        String definition2 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME
                + " ("
                + "    __key BIGINT,"
                + "    \"timestamp\" VARCHAR,"
                + "    symbol VARCHAR,"
                + "    volume BIGINT"
                + ")"
                 + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.Long',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";

        String definition3 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_KAFKA_CONFIG
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + String.class.getCanonicalName() + "'"
                + " )";

        String definition4 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_SYMBOLS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + SymbolInfo.class.getCanonicalName() + "'"
                + " )";

        String definition5 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRADES
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Trade.class.getCanonicalName() + "'"
                + " )";

        define(definition1, hazelcastInstance);
        define(definition2, hazelcastInstance);
        define(definition3, hazelcastInstance);
        define(definition4, hazelcastInstance);
        define(definition5, hazelcastInstance);
    }

    /**
     * <p>More map definitions
     * </p>
     * @param hazelcastInstance
     */
     static void defineIMap2(HazelcastInstance hazelcastInstance) {
        String definition6 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_PYTHON_SENTIMENT
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String'"
                + " )";


        define(definition6, hazelcastInstance);
    }


    /**
     * <p>Generic handler to loading definitions
     * </p>
     *
     * @param definition
     * @param hazelcastInstance
     */
    static void define(String definition, HazelcastInstance hazelcastInstance) {
        LOGGER.info("Definition '{}'", definition);
        try {
            hazelcastInstance.getSql().execute(definition);
        } catch (Exception e) {
            LOGGER.error(definition, e);
        }
    }


    static void launchNeededJobs(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        // Only do this for the first node.
        if (hazelcastInstance.getCluster().getMembers().size() != 1) {
            return;
        }

        if (!System.getProperty("my.autostart.enabled", "").equalsIgnoreCase("true")) {
            LOGGER.info("Not launching Kafka jobs automatically at cluster creation: 'my.autostart.enabled'=='{}'",
                    System.getProperty("my.autostart.enabled"));
        } else {
            LOGGER.info("Launching Kafka jobs automatically at cluster creation: 'my.autostart.enabled'=='{}'",
                    System.getProperty("my.autostart.enabled"));

            // Trade ingest
            Pipeline pipelineIngestTrades = IngestTrades.buildPipeline(bootstrapServers);

            JobConfig jobConfigIngestTrades = new JobConfig();
            jobConfigIngestTrades.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigIngestTrades.setName(IngestTrades.class.getSimpleName());

            hazelcastInstance.getJet().newJobIfAbsent(pipelineIngestTrades, jobConfigIngestTrades);

            // Trade aggregation
            Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(bootstrapServers);

            JobConfig jobConfigAggregateQuery = new JobConfig();
            jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName());
            jobConfigAggregateQuery.addClass(MaxVolumeAggregator.class);

            hazelcastInstance.getJet().newJobIfAbsent(pipelineAggregateQuery, jobConfigAggregateQuery);
        }

        // Remaining jobs need properties, skip if not as expected

        Properties properties = null;
        try {
            properties = UtilsProperties.loadClasspathProperties(APPLICATION_PROPERTIES_FILE);
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName(), e);
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName()
                    + " - No jobs submitted for Slack");
            return;
        }

        // Slack SQL integration from common utils
        try {
            Object projectName = properties.get(UtilsConstants.SLACK_PROJECT_NAME);

        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName(), e);
        }

        // Slack alerting, indirectly uses common utils
//        try {
//            Pipeline pipelineAlertingToSlack = AlertingToSlack.buildPipeline(
//                    properties.get(UtilsConstants.SLACK_ACCESS_TOKEN),
//                    properties.get(UtilsConstants.SLACK_CHANNEL_NAME),
//                    properties.get(UtilsConstants.SLACK_PROJECT_NAME)
//                    );
//
//            JobConfig jobConfigAlertingToSlack = new JobConfig();
//            jobConfigAlertingToSlack.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
//            jobConfigAlertingToSlack.setName(AlertingToSlack.class.getSimpleName());
//            jobConfigAlertingToSlack.addClass(AlertingToSlack.class);
//            jobConfigAlertingToSlack.addClass(UtilsSlackSink.class);
//
//            hazelcastInstance.getJet().newJobIfAbsent(pipelineAlertingToSlack, jobConfigAlertingToSlack);
//        } catch (Exception e) {
//            LOGGER.error("launchNeededJobs:" + AlertingToSlack.class.getSimpleName(), e);
//        }
    }

}
