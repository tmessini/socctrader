package com.socctrader.web.rest;

import com.socctrader.SocctraderApp;
import com.socctrader.domain.League;
import com.socctrader.repository.LeagueRepository;
import com.socctrader.repository.search.LeagueSearchRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for the LeagueResource REST controller.
 *
 * @see LeagueResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SocctraderApp.class)
@WebAppConfiguration
@IntegrationTest
public class LeagueResourceIntTest {


    private static final Long DEFAULT_LEAGUE_ID = 1L;
    private static final Long UPDATED_LEAGUE_ID = 2L;
    private static final String DEFAULT_LEAGUE_NAME = "AAAAA";
    private static final String UPDATED_LEAGUE_NAME = "BBBBB";

    @Inject
    private LeagueRepository leagueRepository;

    @Inject
    private LeagueSearchRepository leagueSearchRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restLeagueMockMvc;

    private League league;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        LeagueResource leagueResource = new LeagueResource();
        ReflectionTestUtils.setField(leagueResource, "leagueSearchRepository", leagueSearchRepository);
        ReflectionTestUtils.setField(leagueResource, "leagueRepository", leagueRepository);
        this.restLeagueMockMvc = MockMvcBuilders.standaloneSetup(leagueResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        leagueSearchRepository.deleteAll();
        league = new League();
        league.setLeagueId(DEFAULT_LEAGUE_ID);
        league.setLeagueName(DEFAULT_LEAGUE_NAME);
    }

    @Test
    @Transactional
    public void createLeague() throws Exception {
        int databaseSizeBeforeCreate = leagueRepository.findAll().size();

        // Create the League

        restLeagueMockMvc.perform(post("/api/leagues")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(league)))
                .andExpect(status().isCreated());

        // Validate the League in the database
        List<League> leagues = leagueRepository.findAll();
        assertThat(leagues).hasSize(databaseSizeBeforeCreate + 1);
        League testLeague = leagues.get(leagues.size() - 1);
        assertThat(testLeague.getLeagueId()).isEqualTo(DEFAULT_LEAGUE_ID);
        assertThat(testLeague.getLeagueName()).isEqualTo(DEFAULT_LEAGUE_NAME);

        // Validate the League in ElasticSearch
        League leagueEs = leagueSearchRepository.findOne(testLeague.getId());
        assertThat(leagueEs).isEqualToComparingFieldByField(testLeague);
    }

    @Test
    @Transactional
    public void checkLeagueNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = leagueRepository.findAll().size();
        // set the field null
        league.setLeagueName(null);

        // Create the League, which fails.

        restLeagueMockMvc.perform(post("/api/leagues")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(league)))
                .andExpect(status().isBadRequest());

        List<League> leagues = leagueRepository.findAll();
        assertThat(leagues).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllLeagues() throws Exception {
        // Initialize the database
        leagueRepository.saveAndFlush(league);

        // Get all the leagues
        restLeagueMockMvc.perform(get("/api/leagues?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(league.getId().intValue())))
                .andExpect(jsonPath("$.[*].leagueId").value(hasItem(DEFAULT_LEAGUE_ID.intValue())))
                .andExpect(jsonPath("$.[*].leagueName").value(hasItem(DEFAULT_LEAGUE_NAME.toString())));
    }

    @Test
    @Transactional
    public void getLeague() throws Exception {
        // Initialize the database
        leagueRepository.saveAndFlush(league);

        // Get the league
        restLeagueMockMvc.perform(get("/api/leagues/{id}", league.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(league.getId().intValue()))
            .andExpect(jsonPath("$.leagueId").value(DEFAULT_LEAGUE_ID.intValue()))
            .andExpect(jsonPath("$.leagueName").value(DEFAULT_LEAGUE_NAME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingLeague() throws Exception {
        // Get the league
        restLeagueMockMvc.perform(get("/api/leagues/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateLeague() throws Exception {
        // Initialize the database
        leagueRepository.saveAndFlush(league);
        leagueSearchRepository.save(league);
        int databaseSizeBeforeUpdate = leagueRepository.findAll().size();

        // Update the league
        League updatedLeague = new League();
        updatedLeague.setId(league.getId());
        updatedLeague.setLeagueId(UPDATED_LEAGUE_ID);
        updatedLeague.setLeagueName(UPDATED_LEAGUE_NAME);

        restLeagueMockMvc.perform(put("/api/leagues")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedLeague)))
                .andExpect(status().isOk());

        // Validate the League in the database
        List<League> leagues = leagueRepository.findAll();
        assertThat(leagues).hasSize(databaseSizeBeforeUpdate);
        League testLeague = leagues.get(leagues.size() - 1);
        assertThat(testLeague.getLeagueId()).isEqualTo(UPDATED_LEAGUE_ID);
        assertThat(testLeague.getLeagueName()).isEqualTo(UPDATED_LEAGUE_NAME);

        // Validate the League in ElasticSearch
        League leagueEs = leagueSearchRepository.findOne(testLeague.getId());
        assertThat(leagueEs).isEqualToComparingFieldByField(testLeague);
    }

    @Test
    @Transactional
    public void deleteLeague() throws Exception {
        // Initialize the database
        leagueRepository.saveAndFlush(league);
        leagueSearchRepository.save(league);
        int databaseSizeBeforeDelete = leagueRepository.findAll().size();

        // Get the league
        restLeagueMockMvc.perform(delete("/api/leagues/{id}", league.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate ElasticSearch is empty
        boolean leagueExistsInEs = leagueSearchRepository.exists(league.getId());
        assertThat(leagueExistsInEs).isFalse();

        // Validate the database is empty
        List<League> leagues = leagueRepository.findAll();
        assertThat(leagues).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchLeague() throws Exception {
        // Initialize the database
        leagueRepository.saveAndFlush(league);
        leagueSearchRepository.save(league);

        // Search the league
        restLeagueMockMvc.perform(get("/api/_search/leagues?query=id:" + league.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(league.getId().intValue())))
            .andExpect(jsonPath("$.[*].leagueId").value(hasItem(DEFAULT_LEAGUE_ID.intValue())))
            .andExpect(jsonPath("$.[*].leagueName").value(hasItem(DEFAULT_LEAGUE_NAME.toString())));
    }
}
