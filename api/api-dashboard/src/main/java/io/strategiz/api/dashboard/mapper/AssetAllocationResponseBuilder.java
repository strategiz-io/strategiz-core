package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.assetallocation.AssetAllocationResponse;
import io.strategiz.api.base.model.ResponseMetadata;
import io.strategiz.service.dashboard.model.assetallocation.AssetAllocationData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * MapStruct builder interface for converting service-layer asset allocation DTOs to API-layer response objects.
 * 
 * Implementations are generated automatically by MapStruct at compile time.
 */
@Mapper(componentModel = "spring")
public interface AssetAllocationResponseBuilder {

    AssetAllocationResponseBuilder INSTANCE = Mappers.getMapper(AssetAllocationResponseBuilder.class);

    /**
     * Converts a service layer asset allocation data to an API layer asset allocation response
     */
    @Mapping(target = "metadata", ignore = true)
    AssetAllocationResponse buildAssetAllocationResponse(AssetAllocationData serviceData);
    
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
