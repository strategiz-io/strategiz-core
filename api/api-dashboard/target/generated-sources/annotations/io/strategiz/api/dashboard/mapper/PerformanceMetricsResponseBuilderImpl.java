package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.performancemetrics.PerformanceMetricsResponse;
import io.strategiz.service.dashboard.model.performancemetrics.PerformanceMetricsData;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-31T12:22:59-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class PerformanceMetricsResponseBuilderImpl implements PerformanceMetricsResponseBuilder {

    @Override
    public PerformanceMetricsResponse buildPerformanceMetricsResponse(PerformanceMetricsData serviceData) {
        if ( serviceData == null ) {
            return null;
        }

        PerformanceMetricsResponse performanceMetricsResponse = new PerformanceMetricsResponse();

        performanceMetricsResponse.setSummary( performanceSummaryToPerformanceSummary( serviceData.getSummary() ) );

        performanceMetricsResponse.setHistoricalValues( mapPerformanceSummaryToHistoricalValues(serviceData.getSummary()) );

        return performanceMetricsResponse;
    }

    protected PerformanceMetricsResponse.PerformanceSummary performanceSummaryToPerformanceSummary(PerformanceMetricsData.PerformanceSummary performanceSummary) {
        if ( performanceSummary == null ) {
            return null;
        }

        PerformanceMetricsResponse.PerformanceSummary performanceSummary1 = new PerformanceMetricsResponse.PerformanceSummary();

        performanceSummary1.setTotalProfitLoss( performanceSummary.getTotalProfitLoss() );
        performanceSummary1.setTotalProfitLossPercentage( performanceSummary.getTotalProfitLossPercentage() );
        performanceSummary1.setDailyChange( performanceSummary.getDailyChange() );
        performanceSummary1.setDailyChangePercentage( performanceSummary.getDailyChangePercentage() );
        performanceSummary1.setWeeklyChange( performanceSummary.getWeeklyChange() );
        performanceSummary1.setWeeklyChangePercentage( performanceSummary.getWeeklyChangePercentage() );
        performanceSummary1.setMonthlyChange( performanceSummary.getMonthlyChange() );
        performanceSummary1.setMonthlyChangePercentage( performanceSummary.getMonthlyChangePercentage() );
        performanceSummary1.setYtdChange( performanceSummary.getYtdChange() );
        performanceSummary1.setYtdChangePercentage( performanceSummary.getYtdChangePercentage() );
        performanceSummary1.setProfitable( performanceSummary.isProfitable() );

        return performanceSummary1;
    }
}
