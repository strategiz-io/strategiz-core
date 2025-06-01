package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.riskanalysis.RiskAnalysisResponse;
import io.strategiz.service.dashboard.model.riskanalysis.RiskAnalysisData;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-31T21:22:24-0400",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class RiskAnalysisResponseBuilderImpl implements RiskAnalysisResponseBuilder {

    @Override
    public RiskAnalysisResponse buildRiskAnalysisResponse(RiskAnalysisData serviceData) {
        if ( serviceData == null ) {
            return null;
        }

        RiskAnalysisResponse riskAnalysisResponse = new RiskAnalysisResponse();

        riskAnalysisResponse.setCorrelationMetric( createCorrelationMetric( serviceData.getCorrelationMetric() ) );
        riskAnalysisResponse.setDiversificationMetric( createDiversificationMetric( serviceData.getDiversificationMetric() ) );
        riskAnalysisResponse.setVolatilityMetric( createVolatilityMetric( serviceData.getVolatilityMetric() ) );

        return riskAnalysisResponse;
    }
}
