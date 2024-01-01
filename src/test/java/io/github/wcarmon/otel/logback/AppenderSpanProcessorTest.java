package io.github.wcarmon.otel.logback;


import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

final class AppenderSpanProcessorTest {

    @Test
    void appender() {

        final var log = LoggerFactory.getLogger(AppenderSpanProcessorTest.class);

        final var converter = OtelToLogback.builder()
                .loggerName("otelLogback")
                .defaultLevel(Level.INFO)
                .build();

        final var sp = LogbackSpanProcessor.builder()
                .targetAppenderName("myAppender")
                .converter(converter)
                .build();

        // TODO: use SpanProcessor here
    }
}
