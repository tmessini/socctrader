package com.socctrader.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.socctrader.domain.League;
import com.socctrader.repository.LeagueRepository;
import com.socctrader.repository.search.LeagueSearchRepository;
import com.socctrader.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing League.
 */
@RestController
@RequestMapping("/api")
public class LeagueResource {

    private final Logger log = LoggerFactory.getLogger(LeagueResource.class);
        
    @Inject
    private LeagueRepository leagueRepository;
    
    @Inject
    private LeagueSearchRepository leagueSearchRepository;
    
    /**
     * POST  /leagues : Create a new league.
     *
     * @param league the league to create
     * @return the ResponseEntity with status 201 (Created) and with body the new league, or with status 400 (Bad Request) if the league has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/leagues",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<League> createLeague(@Valid @RequestBody League league) throws URISyntaxException {
        log.debug("REST request to save League : {}", league);
        if (league.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("league", "idexists", "A new league cannot already have an ID")).body(null);
        }
        League result = leagueRepository.save(league);
        leagueSearchRepository.save(result);
        return ResponseEntity.created(new URI("/api/leagues/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("league", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /leagues : Updates an existing league.
     *
     * @param league the league to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated league,
     * or with status 400 (Bad Request) if the league is not valid,
     * or with status 500 (Internal Server Error) if the league couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/leagues",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<League> updateLeague(@Valid @RequestBody League league) throws URISyntaxException {
        log.debug("REST request to update League : {}", league);
        if (league.getId() == null) {
            return createLeague(league);
        }
        League result = leagueRepository.save(league);
        leagueSearchRepository.save(result);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("league", league.getId().toString()))
            .body(result);
    }

    /**
     * GET  /leagues : get all the leagues.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of leagues in body
     */
    @RequestMapping(value = "/leagues",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<League> getAllLeagues() {
        log.debug("REST request to get all Leagues");
        List<League> leagues = leagueRepository.findAll();
        return leagues;
    }

    /**
     * GET  /leagues/:id : get the "id" league.
     *
     * @param id the id of the league to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the league, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/leagues/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<League> getLeague(@PathVariable Long id) {
        log.debug("REST request to get League : {}", id);
        League league = leagueRepository.findOne(id);
        return Optional.ofNullable(league)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /leagues/:id : delete the "id" league.
     *
     * @param id the id of the league to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/leagues/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteLeague(@PathVariable Long id) {
        log.debug("REST request to delete League : {}", id);
        leagueRepository.delete(id);
        leagueSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("league", id.toString())).build();
    }

    /**
     * SEARCH  /_search/leagues?query=:query : search for the league corresponding
     * to the query.
     *
     * @param query the query of the league search
     * @return the result of the search
     */
    @RequestMapping(value = "/_search/leagues",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<League> searchLeagues(@RequestParam String query) {
        log.debug("REST request to search Leagues for query {}", query);
        return StreamSupport
            .stream(leagueSearchRepository.search(queryStringQuery(query)).spliterator(), false)
            .collect(Collectors.toList());
    }


}
