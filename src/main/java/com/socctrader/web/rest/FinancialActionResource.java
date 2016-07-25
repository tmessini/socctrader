package com.socctrader.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.socctrader.domain.FinancialAction;
import com.socctrader.repository.FinancialActionRepository;
import com.socctrader.repository.search.FinancialActionSearchRepository;
import com.socctrader.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing FinancialAction.
 */
@RestController
@RequestMapping("/api")
public class FinancialActionResource {

    private final Logger log = LoggerFactory.getLogger(FinancialActionResource.class);
        
    @Inject
    private FinancialActionRepository financialActionRepository;
    
    @Inject
    private FinancialActionSearchRepository financialActionSearchRepository;
    
    /**
     * POST  /financial-actions : Create a new financialAction.
     *
     * @param financialAction the financialAction to create
     * @return the ResponseEntity with status 201 (Created) and with body the new financialAction, or with status 400 (Bad Request) if the financialAction has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/financial-actions",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<FinancialAction> createFinancialAction(@RequestBody FinancialAction financialAction) throws URISyntaxException {
        log.debug("REST request to save FinancialAction : {}", financialAction);
        if (financialAction.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("financialAction", "idexists", "A new financialAction cannot already have an ID")).body(null);
        }
        FinancialAction result = financialActionRepository.save(financialAction);
        financialActionSearchRepository.save(result);
        return ResponseEntity.created(new URI("/api/financial-actions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("financialAction", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /financial-actions : Updates an existing financialAction.
     *
     * @param financialAction the financialAction to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated financialAction,
     * or with status 400 (Bad Request) if the financialAction is not valid,
     * or with status 500 (Internal Server Error) if the financialAction couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/financial-actions",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<FinancialAction> updateFinancialAction(@RequestBody FinancialAction financialAction) throws URISyntaxException {
        log.debug("REST request to update FinancialAction : {}", financialAction);
        if (financialAction.getId() == null) {
            return createFinancialAction(financialAction);
        }
        FinancialAction result = financialActionRepository.save(financialAction);
        financialActionSearchRepository.save(result);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("financialAction", financialAction.getId().toString()))
            .body(result);
    }

    /**
     * GET  /financial-actions : get all the financialActions.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of financialActions in body
     */
    @RequestMapping(value = "/financial-actions",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<FinancialAction> getAllFinancialActions() {
        log.debug("REST request to get all FinancialActions");
        List<FinancialAction> financialActions = financialActionRepository.findAll();
        return financialActions;
    }

    /**
     * GET  /financial-actions/:id : get the "id" financialAction.
     *
     * @param id the id of the financialAction to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the financialAction, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/financial-actions/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<FinancialAction> getFinancialAction(@PathVariable Long id) {
        log.debug("REST request to get FinancialAction : {}", id);
        FinancialAction financialAction = financialActionRepository.findOne(id);
        return Optional.ofNullable(financialAction)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /financial-actions/:id : delete the "id" financialAction.
     *
     * @param id the id of the financialAction to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/financial-actions/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteFinancialAction(@PathVariable Long id) {
        log.debug("REST request to delete FinancialAction : {}", id);
        financialActionRepository.delete(id);
        financialActionSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("financialAction", id.toString())).build();
    }

    /**
     * SEARCH  /_search/financial-actions?query=:query : search for the financialAction corresponding
     * to the query.
     *
     * @param query the query of the financialAction search
     * @return the result of the search
     */
    @RequestMapping(value = "/_search/financial-actions",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<FinancialAction> searchFinancialActions(@RequestParam String query) {
        log.debug("REST request to search FinancialActions for query {}", query);
        return StreamSupport
            .stream(financialActionSearchRepository.search(queryStringQuery(query)).spliterator(), false)
            .collect(Collectors.toList());
    }


}
