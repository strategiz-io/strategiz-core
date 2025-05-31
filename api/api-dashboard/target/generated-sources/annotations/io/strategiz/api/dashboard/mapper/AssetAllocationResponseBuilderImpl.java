package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.assetallocation.AssetAllocationResponse;
import io.strategiz.service.dashboard.model.assetallocation.AssetAllocation;
import io.strategiz.service.dashboard.model.assetallocation.AssetAllocationData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-31T17:04:24-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class AssetAllocationResponseBuilderImpl implements AssetAllocationResponseBuilder {

    @Override
    public AssetAllocationResponse buildAssetAllocationResponse(AssetAllocationData serviceData) {
        if ( serviceData == null ) {
            return null;
        }

        AssetAllocationResponse assetAllocationResponse = new AssetAllocationResponse();

        assetAllocationResponse.setAllocations( assetAllocationListToAssetAllocationList( serviceData.getAllocations() ) );

        return assetAllocationResponse;
    }

    protected AssetAllocationResponse.AssetAllocation assetAllocationToAssetAllocation(AssetAllocation assetAllocation) {
        if ( assetAllocation == null ) {
            return null;
        }

        AssetAllocationResponse.AssetAllocation assetAllocation1 = new AssetAllocationResponse.AssetAllocation();

        assetAllocation1.setName( assetAllocation.getName() );
        assetAllocation1.setSymbol( assetAllocation.getSymbol() );
        assetAllocation1.setValue( assetAllocation.getValue() );
        assetAllocation1.setPercentage( assetAllocation.getPercentage() );
        assetAllocation1.setExchange( assetAllocation.getExchange() );
        assetAllocation1.setColor( assetAllocation.getColor() );

        return assetAllocation1;
    }

    protected List<AssetAllocationResponse.AssetAllocation> assetAllocationListToAssetAllocationList(List<AssetAllocation> list) {
        if ( list == null ) {
            return null;
        }

        List<AssetAllocationResponse.AssetAllocation> list1 = new ArrayList<AssetAllocationResponse.AssetAllocation>( list.size() );
        for ( AssetAllocation assetAllocation : list ) {
            list1.add( assetAllocationToAssetAllocation( assetAllocation ) );
        }

        return list1;
    }
}
