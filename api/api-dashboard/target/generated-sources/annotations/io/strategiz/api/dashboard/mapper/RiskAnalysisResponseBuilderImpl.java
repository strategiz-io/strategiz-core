package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.riskanalysis.RiskAnalysisResponse;
import io.strategiz.service.dashboard.model.riskanalysis.RiskAnalysisData;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-31T22:16:45-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class RiskAnalysisResponseBuilderImpl implements RiskAnalysisResponseBuilder {

    @Override
    public RiskAnalysisResponse buildRiskAnalysisResponse(RiskAnalysisData serviceData) {
        if ( serviceData == null ) {
            return null;
        }

        RiskAnalysisResponse riskAnalysisResponse = new RiskAnalysisResponse();

        riskAnalysisResponse.setVolatilityMetric( createVolatilityMetric( serviceData.getVolatilityMetric() ) );
        riskAnalysisResponse.setDiversificationMetric( createDiversificationMetric( serviceData.getDiversificationMetric() ) );
        riskAnalysisResponse.setCorrelationMetric( createCorrelationMetric( serviceData.getCorrelationMetric() ) );

        return riskAnalysisResponse;
    }
}
