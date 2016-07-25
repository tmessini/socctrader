package com.socctrader.repository.search;

import com.socctrader.domain.FinancialAction;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data ElasticSearch repository for the FinancialAction entity.
 */
public interface FinancialActionSearchRepository extends ElasticsearchRepository<FinancialAction, Long> {
}
