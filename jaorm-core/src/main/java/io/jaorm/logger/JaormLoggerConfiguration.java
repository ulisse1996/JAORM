package io.jaorm.logger;

import io.jaorm.spi.common.Singleton;

import java.util.*;
import java.util.logging.*;

public class JaormLoggerConfiguration {

    private static final Singleton<JaormLoggerConfiguration> INSTANCE = Singleton.instance();

    private final Level level;
    private final List<Handler> handlers;
    private final Filter filter;

    public JaormLoggerConfiguration(Builder builder) {
        this.level = Optional.ofNullable(builder.level).orElse(Level.INFO);
        this.handlers = builder.handlers;
        this.filter = Optional.ofNullable(builder.filter).orElse(JaormLogFilter.INSTANCE);
    }

    public Level getLevel() {
        return level;
    }

    public List<Handler> getHandlers() {
        return handlers;
    }

    public Filter getFilter() {
        return filter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized JaormLoggerConfiguration getCurrent() {
        if (!INSTANCE.isPresent()) {
            INSTANCE.set(defaultConfiguration());
        }

        return INSTANCE.get();
    }

    public static synchronized void setCurrent(JaormLoggerConfiguration loggerConfiguration) {
        INSTANCE.set(Objects.requireNonNull(loggerConfiguration, "Configuration can't be null !"));

        // Reset pre started logger
        Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String name = loggerNames.nextElement();
            if (name.startsWith("io.jaorm")) {
                Logger logger = Logger.getLogger(name);
                logger.setLevel(loggerConfiguration.level);
                logger.setFilter(logger.getFilter());
                for (Handler h : logger.getHandlers()) {
                    logger.removeHandler(h);
                }
                loggerConfiguration.handlers.forEach(logger::addHandler);
            }
        }
    }

    private static JaormLoggerConfiguration defaultConfiguration() {
        return JaormLoggerConfiguration.builder()
                .setLevel(Level.FINE)
                .setFilter(JaormLogFilter.INSTANCE)
                .addHandler(new ConsoleHandler())
                .build();
    }

    public static class Builder {

        private Level level;
        private final List<Handler> handlers = new ArrayList<>();
        private Filter filter;

        public Builder setLevel(Level level) {
            this.level = level;
            return this;
        }

        public Builder addHandler(Handler handler) {
            this.handlers.add(handler);
            return this;
        }

        public Builder setFilter(Filter filter) {
            this.filter = filter;
            return this;
        }

        public JaormLoggerConfiguration build() {
            return new JaormLoggerConfiguration(this);
        }
    }
}
