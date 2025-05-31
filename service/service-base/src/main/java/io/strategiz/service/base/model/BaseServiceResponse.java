package io.strategiz.service.base.model;

import java.io.Serializable;
import java.util.Map;

/**
 * Base class for service response objects.
 */
public abstract class BaseServiceResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Metadata map for response information
     */
    private Map<String, Object> metadata;
    
    /**
     * Gets the metadata for this response
     * 
     * @return metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Sets the metadata for this response
     * 
     * @param metadata metadata map
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
