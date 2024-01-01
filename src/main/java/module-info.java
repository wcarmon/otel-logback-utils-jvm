/** module decl for otel */
module io.github.wcarmon.otel.logback {
    exports io.github.wcarmon.otel.logback;

    // TODO: prune unused
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    requires io.opentelemetry.sdk.trace;
    requires org.jetbrains.annotations;
    requires org.slf4j;
}
