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

package com.hazelcast.platform.demos.banking.trademonitor;



import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.map.IMap;
import com.hazelcast.partition.PartitionService;


public class CountIMapPartitionsCallable implements Callable<Map<Integer, Integer>>,
    HazelcastInstanceAware, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CountIMapPartitionsCallable.class);

    private transient HazelcastInstance hazelcastInstance;


    @Override
    public Map<Integer, Integer> call() throws Exception {
        final Map<Integer, Integer> result = new HashMap<>();
        final PartitionService partitionService = this.hazelcastInstance.getPartitionService();

        try {
            this.hazelcastInstance.getDistributedObjects()
            .stream()
            .filter(distributedObject -> (distributedObject instanceof IMap))
            .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
            .map(distributedObject -> ((IMap<?, ?>) distributedObject))
            .forEach(iMap -> {
                iMap.localKeySet()
                .forEach(key -> {
                    int partitionId = partitionService.getPartition(key).getPartitionId();
                    result.merge(partitionId, 1, Integer::sum);
                });
            });
        } catch (Exception e) {
            LOGGER.error("call()", e);
        }

        return result;
    }


    @Override
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
