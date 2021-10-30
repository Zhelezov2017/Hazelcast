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

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);


    static {
        System.setProperty("hazelcast.logging.type", "slf4j");
        System.setProperty("my.autostart.enabled", "true");
    }


    public static void main(String[] args) throws Exception {
        String bootstrapServers = "127.0.0.1:9092";

        Config config = ApplicationConfig.buildConfig();

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        ApplicationInitializer.initialise(hazelcastInstance, bootstrapServers);
    }

}
