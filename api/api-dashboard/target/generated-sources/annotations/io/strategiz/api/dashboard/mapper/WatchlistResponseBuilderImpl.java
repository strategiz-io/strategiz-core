package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.watchlist.WatchlistResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-31T23:07:07-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class WatchlistResponseBuilderImpl implements WatchlistResponseBuilder {

    @Override
    public WatchlistResponse buildWatchlistResponse(io.strategiz.service.dashboard.model.watchlist.WatchlistResponse response) {
        if ( response == null ) {
            return null;
        }

        WatchlistResponse watchlistResponse = new WatchlistResponse();

        watchlistResponse.setWatchlistItems( watchlistItemListToWatchlistItemList( response.getAssets() ) );
        List<String> list1 = response.getAvailableCategories();
        if ( list1 != null ) {
            watchlistResponse.setAvailableCategories( new ArrayList<String>( list1 ) );
        }

        return watchlistResponse;
    }

    protected WatchlistResponse.WatchlistItem watchlistItemToWatchlistItem(io.strategiz.service.dashboard.model.watchlist.WatchlistResponse.WatchlistItem watchlistItem) {
        if ( watchlistItem == null ) {
            return null;
        }

        WatchlistResponse.WatchlistItem watchlistItem1 = new WatchlistResponse.WatchlistItem();

        watchlistItem1.setId( watchlistItem.getId() );
        watchlistItem1.setSymbol( watchlistItem.getSymbol() );
        watchlistItem1.setName( watchlistItem.getName() );
        watchlistItem1.setCategory( watchlistItem.getCategory() );
        watchlistItem1.setPrice( watchlistItem.getPrice() );
        watchlistItem1.setChange( watchlistItem.getChange() );
        watchlistItem1.setChangePercent( watchlistItem.getChangePercent() );
        watchlistItem1.setPositiveChange( watchlistItem.isPositiveChange() );
        watchlistItem1.setChartDataUrl( watchlistItem.getChartDataUrl() );

        return watchlistItem1;
    }

    protected List<WatchlistResponse.WatchlistItem> watchlistItemListToWatchlistItemList(List<io.strategiz.service.dashboard.model.watchlist.WatchlistResponse.WatchlistItem> list) {
        if ( list == null ) {
            return null;
        }

        List<WatchlistResponse.WatchlistItem> list1 = new ArrayList<WatchlistResponse.WatchlistItem>( list.size() );
        for ( io.strategiz.service.dashboard.model.watchlist.WatchlistResponse.WatchlistItem watchlistItem : list ) {
            list1.add( watchlistItemToWatchlistItem( watchlistItem ) );
        }

        return list1;
    }
}
