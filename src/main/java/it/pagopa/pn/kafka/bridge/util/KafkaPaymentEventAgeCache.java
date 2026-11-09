package it.pagopa.pn.kafka.bridge.util;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class KafkaPaymentEventAgeCache {

    private final ConcurrentHashMap<Integer, Long> cache = new ConcurrentHashMap<>();

    /** Tiene il valore massimo per partizione nella finestra corrente. */
    public void recordAge(Integer partition, long ageMillis) {
        cache.merge(partition, ageMillis, Math::max);
    }

    /** Svuota la cache in modo atomico per chiave e restituisce i valori rimossi. */
    public Map<Integer, Long> drain() {
        Map<Integer, Long> drained = new HashMap<>();
        for (Integer key : cache.keySet()) {
            Long value = cache.remove(key);
            if (value != null) drained.put(key, value);
        }
        return drained;
    }

    public Long get(Integer key)              { return cache.get(key); }
    public Long remove(Integer key)           { return cache.remove(key); }
    public Set<Integer> keys()                { return cache.keySet(); }
    public Map<Integer, Long> snapshot()      { return Map.copyOf(cache); }
    public int size()                        { return cache.size(); }
    public boolean isEmpty()                 { return cache.isEmpty(); }
    public void clear()                      { cache.clear(); }
}