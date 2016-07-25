package com.socctrader.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.socctrader.domain.Team;
import com.socctrader.repository.TeamRepository;
import com.socctrader.repository.search.TeamSearchRepository;
import com.socctrader.web.rest.util.HeaderUtil;
import com.socctrader.web.rest.util.PaginationUtil;
import com.socctrader.web.rest.dto.TeamDTO;
import com.socctrader.web.rest.mapper.TeamMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing Team.
 */
@RestController
@RequestMapping("/api")
public class TeamResource {

    private final Logger log = LoggerFactory.getLogger(TeamResource.class);
        
    @Inject
    private TeamRepository teamRepository;
    
    @Inject
    private TeamMapper teamMapper;
    
    @Inject
    private TeamSearchRepository teamSearchRepository;
    
    /**
     * POST  /teams : Create a new team.
     *
     * @param teamDTO the teamDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new teamDTO, or with status 400 (Bad Request) if the team has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/teams",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<TeamDTO> createTeam(@RequestBody TeamDTO teamDTO) throws URISyntaxException {
        log.debug("REST request to save Team : {}", teamDTO);
        if (teamDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("team", "idexists", "A new team cannot already have an ID")).body(null);
        }
        Team team = teamMapper.teamDTOToTeam(teamDTO);
        team = teamRepository.save(team);
        TeamDTO result = teamMapper.teamToTeamDTO(team);
        teamSearchRepository.save(team);
        return ResponseEntity.created(new URI("/api/teams/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("team", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /teams : Updates an existing team.
     *
     * @param teamDTO the teamDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated teamDTO,
     * or with status 400 (Bad Request) if the teamDTO is not valid,
     * or with status 500 (Internal Server Error) if the teamDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/teams",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<TeamDTO> updateTeam(@RequestBody TeamDTO teamDTO) throws URISyntaxException {
        log.debug("REST request to update Team : {}", teamDTO);
        if (teamDTO.getId() == null) {
            return createTeam(teamDTO);
        }
        Team team = teamMapper.teamDTOToTeam(teamDTO);
        team = teamRepository.save(team);
        TeamDTO result = teamMapper.teamToTeamDTO(team);
        teamSearchRepository.save(team);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("team", teamDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /teams : get all the teams.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of teams in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/teams",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<TeamDTO>> getAllTeams(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Teams");
        Page<Team> page = teamRepository.findAll(pageable); 
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/teams");
        return new ResponseEntity<>(teamMapper.teamsToTeamDTOs(page.getContent()), headers, HttpStatus.OK);
    }

    /**
     * GET  /teams/:id : get the "id" team.
     *
     * @param id the id of the teamDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the teamDTO, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/teams/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<TeamDTO> getTeam(@PathVariable Long id) {
        log.debug("REST request to get Team : {}", id);
        Team team = teamRepository.findOne(id);
        TeamDTO teamDTO = teamMapper.teamToTeamDTO(team);
        return Optional.ofNullable(teamDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /teams/:id : delete the "id" team.
     *
     * @param id the id of the teamDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/teams/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        log.debug("REST request to delete Team : {}", id);
        teamRepository.delete(id);
        teamSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("team", id.toString())).build();
    }

    /**
     * SEARCH  /_search/teams?query=:query : search for the team corresponding
     * to the query.
     *
     * @param query the query of the team search
     * @return the result of the search
     */
    @RequestMapping(value = "/_search/teams",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<TeamDTO>> searchTeams(@RequestParam String query, Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to search for a page of Teams for query {}", query);
        Page<Team> page = teamSearchRepository.search(queryStringQuery(query), pageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/teams");
        return new ResponseEntity<>(teamMapper.teamsToTeamDTOs(page.getContent()), headers, HttpStatus.OK);
    }


}
