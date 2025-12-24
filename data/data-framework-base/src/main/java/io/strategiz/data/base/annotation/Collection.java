package io.strategiz.data.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the Firestore collection name for an entity.
 * This replaces the getCollectionName() method to avoid Firestore 
 * accidentally storing it as a field.
 * 
 * @author Strategiz Platform
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Collection {
    /**
     * The name of the Firestore collection
     */
    String value();
}