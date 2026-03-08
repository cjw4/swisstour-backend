package dg.swiss.swiss_dg_db.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EntityScan("dg.swiss.swiss_dg_db")
@EnableJpaRepositories("dg.swiss.swiss_dg_db")
@EnableTransactionManagement
public class DomainConfig {}
