package io.strategiz.data.framework.timescale.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "strategiz.timescale.enabled", havingValue = "true")
public class TimescaleTransactionConfig {

	@Bean(name = "timescaleTransactionManager")
	public PlatformTransactionManager timescaleTransactionManager(
			@Qualifier("timescaleDataSource") DataSource dataSource) {

		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
		transactionManager.setDefaultTimeout(300); // 5 minutes default timeout
		return transactionManager;
	}

}
