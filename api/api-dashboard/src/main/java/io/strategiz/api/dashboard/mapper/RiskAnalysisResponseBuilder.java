package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.riskanalysis.RiskAnalysisResponse;
import io.strategiz.service.dashboard.model.riskanalysis.RiskAnalysisData;
import io.strategiz.service.dashboard.model.riskanalysis.VolatilityMetric;
import io.strategiz.service.dashboard.model.riskanalysis.DiversificationMetric;
import io.strategiz.service.dashboard.model.riskanalysis.CorrelationMetric;
import io.strategiz.api.base.model.ResponseMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * MapStruct builder interface for converting service-layer risk analysis DTOs to API-layer response objects.
 * 
 * Implementations are generated automatically by MapStruct at compile time.
 */
@Mapper(componentModel = "spring")
public interface RiskAnalysisResponseBuilder {

    RiskAnalysisResponseBuilder INSTANCE = Mappers.getMapper(RiskAnalysisResponseBuilder.class);

    /**
     * Converts a service layer risk analysis data to an API layer risk analysis response
     */
    @Mapping(target = "metadata", ignore = true)
    RiskAnalysisResponse buildRiskAnalysisResponse(RiskAnalysisData serviceData);
    
    /**
     * Creates a VolatilityMetric from service data
     */
    default RiskAnalysisResponse.VolatilityMetric createVolatilityMetric(VolatilityMetric serviceMetric) {
        if (serviceMetric == null) {
            return null;
        }
        RiskAnalysisResponse.VolatilityMetric volatilityMetric = new RiskAnalysisResponse.VolatilityMetric();
        // Map properties from service metric to API metric
        // Add implementation based on available properties
        return volatilityMetric;
    }
    
    /**
     * Creates a DiversificationMetric from service data
     */
    default RiskAnalysisResponse.DiversificationMetric createDiversificationMetric(DiversificationMetric serviceMetric) {
        if (serviceMetric == null) {
            return null;
        }
        RiskAnalysisResponse.DiversificationMetric diversificationMetric = new RiskAnalysisResponse.DiversificationMetric();
        // Map properties from service metric to API metric
        // Add implementation based on available properties
        return diversificationMetric;
    }
    
    /**
     * Creates a CorrelationMetric from service data
     */
    default RiskAnalysisResponse.CorrelationMetric createCorrelationMetric(CorrelationMetric serviceMetric) {
        if (serviceMetric == null) {
            return null;
        }
        RiskAnalysisResponse.CorrelationMetric correlationMetric = new RiskAnalysisResponse.CorrelationMetric();
        // Map properties from service metric to API metric
        // Add implementation based on available properties
        return correlationMetric;
    }
    
    /**
     * Convert ResponseMetadata to Map<String, Object> for API response
     * 
     * @param metadata The ResponseMetadata object
     * @return Map representation of the metadata
     */
    default Map<String, Object> convertMetadataToMap(ResponseMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("status", metadata.getStatus());
        metadataMap.put("errorCode", metadata.getErrorCode());
        metadataMap.put("errorMessage", metadata.getErrorMessage());
        metadataMap.put("timestamp", metadata.getTimestamp());
        
        return metadataMap;
    }
}
