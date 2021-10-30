package com.hazelcast.demo.trademonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.hazelcast.platform.demos.banking.trademonitor.MyConstants;
import com.hazelcast.platform.demos.banking.trademonitor.MyUtils;
import com.hazelcast.platform.demos.banking.trademonitor.NasdaqFinancialStatus;
import com.hazelcast.platform.demos.banking.trademonitor.NasdaqMarketCategory;
import org.apache.kafka.clients.producer.KafkaProducer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.datamodel.Tuple3;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
public class ApplicationRunnerKafka implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunnerKafka.class);

    private static final long LOG_THRESHOLD = 20_000L;
    private static final int MAX_BATCH_SIZE = 16 * 1024;
    private static final int OPENING_PRICE = 2_500;
    private static final int LOWEST_QUANTITY = 10;
    private static final int HIGHEST_QUANTITY = 10_000;

    private static final int DEFAULT_RATE = 300;
    private static final int DEFAULT_MAX = -1;
    private final int rate;
    private final int max;
    private int count;
    private final KafkaProducer<String, String> kafkaProducer;
    private final List<String> symbols;
    private final Map<String, Integer> symbolToPrice;

    public ApplicationRunnerKafka(KafkaProducer<String, String> kafkaProducer) throws Exception {
        this.rate = DEFAULT_RATE;
        this.max = DEFAULT_MAX;
        this.kafkaProducer = kafkaProducer;

        Map<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>
            nasdaqListed = MyUtils.nasdaqListed();

        this.symbols = new ArrayList<>(nasdaqListed.keySet());

        this.symbolToPrice = nasdaqListed.entrySet().stream()
                .collect(Collectors.<Entry<String,
                        Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>,
                            String, Integer>toMap(
                        entry -> entry.getKey(),
                        entry -> OPENING_PRICE));
    }


    @Override
    public void run(ApplicationArguments args) {
        {
            if (this.max > 0) {
                LOGGER.info("Producing {} trades per second, until {} written", this.rate, this.max);
            } else {
                LOGGER.info("Producing {} trades per second", this.rate);
            }

            long interval = TimeUnit.SECONDS.toNanos(1) / this.rate;
            long emitSchedule = System.nanoTime();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            // loop over ( wait, create a random trade Id, emit )
            try {
                while (this.max <= 0 || this.count < this.max) {
                    for (int i = 0; i < MAX_BATCH_SIZE; i++) {
                        if (System.nanoTime() < emitSchedule) {
                            break;
                        }

                        String id = UUID.randomUUID().toString();
                        String trade = this.createTrade(id, random);

                        this.kafkaProducer.send(new ProducerRecord<>(MyConstants.KAFKA_TOPIC_NAME_TRADES, id, trade));

                        if (this.count % LOG_THRESHOLD == 0) {
                            LOGGER.info("Wrote {} => \"{}\"", this.count, trade);
                        }
                        this.count++;

                        emitSchedule += interval;
                    }

                    TimeUnit.MILLISECONDS.sleep(1L);
                }
            } catch (InterruptedException exception) {
                this.kafkaProducer.close();
            }

            LOGGER.info("Produced {} trades", this.count);
        }
    }

    private String createTrade(String id, ThreadLocalRandom random) {
        String symbol = symbols.get(random.nextInt(symbols.size()));

        // Vary price between -1 to +2... randomly
        int price = this.symbolToPrice.compute(symbol,
            (k, v) -> v + random.nextInt(-1, 2));

        return String.format("{"
                + "\"id\": \"%s\","
                + "\"timestamp\": %d,"
                + "\"symbol\": \"%s\","
                + "\"price\": %d,"
                + "\"quantity\": %d"
                + "}",
            id,
            System.currentTimeMillis(),
            symbol,
            price,
            random.nextInt(LOWEST_QUANTITY, HIGHEST_QUANTITY)
        );
    }
}
