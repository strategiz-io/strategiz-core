package io.strategiz.robinhood.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RobinhoodService {
    public Object getRawAccountData(String username, String password) {
        // TODO: Implement real Robinhood API integration
        // Use real credentials, fetch and return the unmodified raw data
        // NEVER return mock data
        throw new UnsupportedOperationException("Robinhood API integration not yet implemented");
    }
}
