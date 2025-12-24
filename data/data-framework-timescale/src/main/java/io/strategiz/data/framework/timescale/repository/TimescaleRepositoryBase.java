package io.strategiz.data.framework.timescale.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository interface for all TimescaleDB repositories. Provides standard CRUD
 * operations via Spring Data JPA.
 * @param <T> Entity type
 * @param <ID> ID type
 */
@NoRepositoryBean
public interface TimescaleRepositoryBase<T, ID extends Serializable> extends JpaRepository<T, ID> {

	// Standard JpaRepository methods:
	// - save(entity)
	// - saveAll(entities)
	// - findById(id)
	// - findAll()
	// - findAllById(ids)
	// - count()
	// - delete(entity)
	// - deleteById(id)
	// - deleteAll(entities)

	// Add custom query methods here if needed for all TimescaleDB repositories

}
