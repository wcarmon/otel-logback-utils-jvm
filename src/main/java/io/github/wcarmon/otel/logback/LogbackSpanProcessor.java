package io.github.wcarmon.otel.logback;

import static java.util.Objects.requireNonNull;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.slf4j.LoggerFactory;

/** Forwards all Span events to a Logback appender. */
public final class LogbackSpanProcessor implements SpanProcessor {

    private final Appender<ILoggingEvent> appender;

    private final OtelToLogback converter;

    private LogbackSpanProcessor(OtelToLogback converter, String targetAppenderName) {

        requireNonNull(converter, "converter is required and null.");
        if (targetAppenderName == null || targetAppenderName.isBlank()) {
            throw new IllegalArgumentException("targetAppenderName is required");
        }

        this.converter = converter;

        this.appender = getAppender(targetAppenderName);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {

        final var spanData = span.toSpanData();
        final var events = spanData.getEvents();
        if (events == null || events.isEmpty()) {
            // -- Nothing to log
            return;
        }

        converter.convertEvents(spanData).forEach(appender::doAppend);
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {}

    private Appender<ILoggingEvent> getAppender(String targetAppenderName) {
        if (targetAppenderName == null || targetAppenderName.isBlank()) {
            throw new IllegalArgumentException("targetAppenderName is required");
        }

        final var context = (LoggerContext) LoggerFactory.getILoggerFactory();

        for (var logger : context.getLoggerList()) {
            for (var index = logger.iteratorForAppenders(); index.hasNext(); ) {
                final var appender = index.next();

                if (targetAppenderName.equalsIgnoreCase(appender.getName())) {
                    return appender;
                }
            }
        }

        throw new IllegalStateException(
                "Failed to find logback appender with name='" + targetAppenderName + "'");
    }

    public static class Builder {

        private OtelToLogback converter;
        private String targetAppenderName;

        Builder() {}

        public LogbackSpanProcessor build() {
            return new LogbackSpanProcessor(this.converter, this.targetAppenderName);
        }

        public Builder converter(OtelToLogback converter) {
            this.converter = converter;
            return this;
        }

        public Builder targetAppenderName(String targetAppenderName) {
            this.targetAppenderName = targetAppenderName;
            return this;
        }

        public String toString() {
            return "LogbackSpanProcessor.Builder(converter="
                    + this.converter
                    + ", targetAppenderName="
                    + this.targetAppenderName
                    + ")";
        }
    }
}
