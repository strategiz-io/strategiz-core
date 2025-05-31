package io.strategiz.framework.model;

import java.io.Serializable;

/**
 * Base model class for all model objects in the Strategiz framework.
 * This is an internal implementation to replace the dependency on American Express Synapse framework.
 */
public abstract class BaseModel implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Default constructor.
     */
    protected BaseModel() {
        // Default constructor
    }
    
    /**
     * Returns a string representation of this model.
     *
     * @return a string representation of this model
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
