package io.github.wcarmon.otel.logback;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

/** Converts SpanData to Logback LogEvent. */
public final class OtelToLogback {

    private final Level defaultLevel;

    private final String loggerName;

    private OtelToLogback(String loggerName, @Nullable Level defaultLevel) {

        if (loggerName == null || loggerName.isBlank()) {
            throw new IllegalArgumentException("loggerName is required");
        }

        this.defaultLevel = Objects.requireNonNullElse(defaultLevel, Level.INFO);
        this.loggerName = loggerName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ILoggingEvent convertEvent(EventData spanEvent, SpanData spanData) {
        requireNonNull(spanEvent, "spanEvent is required and null.");

        final var level = getLevel(spanEvent, spanData);

        // TODO: copy attributes to MDC (threading considerations)
        //        spanData.getAttributes()
        //                .forEach((key, value) -> MDC.put(key.getKey(), String.valueOf(value)));

        Exception ex = null;
        if ("exception".equals(spanEvent.getName())) {

            // TODO: improve me
            // spanEvent.exception has the exception, but it's private
            // spanData.getEvents().get(0).exception also has the exception, but it's private
            System.err.println(
                    "TODO: figure out how to get the exception from the spanData or spanEvent");
        }

        final var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);

        Object[] placeholderArgs = null;

        final var out =
                new LoggingEvent(
                        OtelToLogback.class.getName(),
                        logger,
                        level,
                        spanEvent.getName(),
                        ex,
                        placeholderArgs);

        // TODO: set timestamp, requires conversion from epoch nanos to java.time.Instant
        // spanEvent.getEpochNanos()
        // out.setInstant(...);

        return out;
    }

    public List<ILoggingEvent> convertEvents(SpanData spanData) {
        requireNonNull(spanData, "spanData is required and null.");

        if (spanData.getEvents().isEmpty()) {
            throw new IllegalArgumentException("at least one event required");
        }

        return spanData.getEvents().stream()
                .map(spanEvent -> convertEvent(spanEvent, spanData))
                .collect(Collectors.toList());
    }

    private Level getLevel(EventData spanEvent, SpanData spanData) {
        requireNonNull(spanData, "spanData is required and null.");
        requireNonNull(spanEvent, "spanEvent is required and null.");

        if (spanData.getStatus() == StatusData.error()) {
            return Level.ERROR;
        }

        final var rawLevel = getLevelAttributeValue(spanEvent.getAttributes());
        final var desiredLevel = parseLevel(rawLevel);

        if (desiredLevel != null) {
            return desiredLevel;
        }

        return defaultLevel;
    }

    private String getLevelAttributeValue(Attributes attributes) {
        requireNonNull(attributes, "attributes is required and null.");

        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
            final var k = String.valueOf(entry.getKey());
            if ("level".equalsIgnoreCase(k)) {
                return String.valueOf(entry.getValue());
            }
        }

        return "";
    }

    /**
     * handles null, blank, trims, normalizes case
     *
     * @param level "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | (any case, whitespace ignored)
     * @return null only when cannot parse Logback level
     */
    @Nullable
    private Level parseLevel(String level) {

        final var clean = level == null ? "" : level.strip().toUpperCase();

        if (clean.isBlank()) {
            return null;
        }

        return Level.toLevel(clean);
    }

    public static class Builder {

        private @Nullable Level defaultLevel;
        private String loggerName;

        Builder() {
        }

        public OtelToLogback build() {
            return new OtelToLogback(this.loggerName, this.defaultLevel);
        }

        public Builder defaultLevel(@Nullable Level defaultLevel) {
            this.defaultLevel = defaultLevel;
            return this;
        }

        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public String toString() {
            return "OtelToLogback.Builder(loggerName="
                    + this.loggerName
                    + ", defaultLevel="
                    + this.defaultLevel
                    + ")";
        }
    }
}
