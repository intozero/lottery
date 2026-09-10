package com.vipin.lottery.web.config;

import com.vipin.lottery.core.analysis.AnalysisService;
import com.vipin.lottery.core.io.HistoryParser;
import com.vipin.lottery.core.source.PowerballSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring wiring stays out of the framework-independent core. */
@Configuration
public class CoreConfiguration {
    @Bean
    AnalysisService analysisService() {
        return new AnalysisService();
    }

    @Bean
    HistoryParser historyParser() {
        return new HistoryParser();
    }

    @Bean
    PowerballSource powerballSource() {
        return new PowerballSource();
    }
}
