package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.performancemetrics.PerformanceMetricsResponse;
import io.strategiz.api.base.model.ResponseMetadata;
import io.strategiz.service.dashboard.model.performancemetrics.PerformanceMetricsData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapStruct builder interface for converting service-layer performance metrics DTOs to API-layer response objects.
 * 
 * Implementations are generated automatically by MapStruct at compile time.
 */
@Mapper(componentModel = "spring")
public interface PerformanceMetricsResponseBuilder {

    PerformanceMetricsResponseBuilder INSTANCE = Mappers.getMapper(PerformanceMetricsResponseBuilder.class);

    /**
     * Converts a service layer performance metrics data to an API layer performance metrics response
     */
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "historicalValues", expression = "java(mapPerformanceSummaryToHistoricalValues(serviceData.getSummary()))")
    PerformanceMetricsResponse buildPerformanceMetricsResponse(PerformanceMetricsData serviceData);
    
    /**
     * Maps performance summary to historical values list
     */
    default List<PerformanceMetricsResponse.PortfolioValueDataPoint> mapPerformanceSummaryToHistoricalValues(
            PerformanceMetricsData.PerformanceSummary summary) {
        if (summary == null) {
            return new ArrayList<>();
        }
        // Convert the summary to historical values as needed
        // This is a simplified implementation; actual mapping would depend on the data structure
        return new ArrayList<>();
    }
    
    /**
     * Converts ResponseMetadata to a Map
     * 
     * @param metadata Response metadata to convert
     * @return Map representation of metadata
     */
    default Map<String, Object> convertMetadataToMap(ResponseMetadata metadata) {
        if (metadata == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("status", metadata.getStatus());
        metadataMap.put("errorCode", metadata.getErrorCode());
        metadataMap.put("errorMessage", metadata.getErrorMessage());
        metadataMap.put("timestamp", metadata.getTimestamp());
        
        return metadataMap;
    }
}
