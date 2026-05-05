package com.schemaguard.starter.interceptor;

import com.schemaguard.starter.service.SchemaValidationService;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ProducerPostProcessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SchemaGuardProducerPostProcessor<K, V> implements ProducerPostProcessor<K, V> {

    private static final Logger log = LoggerFactory.getLogger(SchemaGuardProducerPostProcessor.class);
    private final SchemaValidationService validationService;

    public SchemaGuardProducerPostProcessor(SchemaValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Producer<K, V> apply(Producer<K, V> producer) {
        return (Producer<K, V>) Proxy.newProxyInstance(
                Producer.class.getClassLoader(),
                new Class<?>[]{Producer.class},
                new ProducerInvocationHandler(producer)
        );
    }

    private class ProducerInvocationHandler implements InvocationHandler {
        private final Producer<K, V> delegate;

        ProducerInvocationHandler(Producer<K, V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("send".equals(method.getName()) && args.length > 0 && args[0] instanceof ProducerRecord) {
                @SuppressWarnings("unchecked")
                ProducerRecord<K, V> record = (ProducerRecord<K, V>) args[0];
                log.trace("Intercepting record via proxy for topic: {}", record.topic());
                
                // Validate and optionally throw SchemaValidationException
                validationService.validate(record.topic(), record.value());
            }

            // Proceed to the underlying Kafka Producer
            try {
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }
}
