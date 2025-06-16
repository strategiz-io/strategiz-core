package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.riskanalysis.RiskAnalysisResponse;
import io.strategiz.service.dashboard.model.riskanalysis.RiskAnalysisData;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-15T22:37:06-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 24.0.1 (Homebrew)"
)
@Component
public class RiskAnalysisResponseBuilderImpl implements RiskAnalysisResponseBuilder {

    @Override
    public RiskAnalysisResponse buildRiskAnalysisResponse(RiskAnalysisData serviceData) {
        if ( serviceData == null ) {
            return null;
        }

        RiskAnalysisResponse riskAnalysisResponse = new RiskAnalysisResponse();

        return riskAnalysisResponse;
    }
}
