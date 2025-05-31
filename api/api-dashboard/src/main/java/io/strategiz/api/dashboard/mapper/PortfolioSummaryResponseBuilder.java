package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse;
import io.strategiz.api.base.model.ResponseMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Map;
import java.util.HashMap;

/**
 * MapStruct builder interface for converting service-layer portfolio summary DTOs to API-layer response objects.
 * 
 * Implementations are generated automatically by MapStruct at compile time.
 */
@Mapper(componentModel = "spring")
public interface PortfolioSummaryResponseBuilder {

    PortfolioSummaryResponseBuilder INSTANCE = Mappers.getMapper(PortfolioSummaryResponseBuilder.class);

    /**
     * Converts a service layer portfolio summary data to an API layer portfolio summary response
     */
    @Mapping(target = "metadata", ignore = true)
    PortfolioSummaryResponse buildPortfolioSummaryResponse(io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse serviceData);
    
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
