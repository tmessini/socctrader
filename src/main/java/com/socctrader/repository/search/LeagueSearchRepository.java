package com.socctrader.repository.search;

import com.socctrader.domain.League;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data ElasticSearch repository for the League entity.
 */
public interface LeagueSearchRepository extends ElasticsearchRepository<League, Long> {
}
