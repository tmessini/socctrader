package com.socctrader.web.rest;

import com.socctrader.SocctraderApp;
import com.socctrader.domain.Team;
import com.socctrader.repository.TeamRepository;
import com.socctrader.repository.search.TeamSearchRepository;
import com.socctrader.web.rest.dto.TeamDTO;
import com.socctrader.web.rest.mapper.TeamMapper;

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
 * Test class for the TeamResource REST controller.
 *
 * @see TeamResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SocctraderApp.class)
@WebAppConfiguration
@IntegrationTest
public class TeamResourceIntTest {


    private static final Long DEFAULT_TEAM_ID = 1L;
    private static final Long UPDATED_TEAM_ID = 2L;
    private static final String DEFAULT_TEAM_NAME = "AAAAA";
    private static final String UPDATED_TEAM_NAME = "BBBBB";

    @Inject
    private TeamRepository teamRepository;

    @Inject
    private TeamMapper teamMapper;

    @Inject
    private TeamSearchRepository teamSearchRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restTeamMockMvc;

    private Team team;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TeamResource teamResource = new TeamResource();
        ReflectionTestUtils.setField(teamResource, "teamSearchRepository", teamSearchRepository);
        ReflectionTestUtils.setField(teamResource, "teamRepository", teamRepository);
        ReflectionTestUtils.setField(teamResource, "teamMapper", teamMapper);
        this.restTeamMockMvc = MockMvcBuilders.standaloneSetup(teamResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        teamSearchRepository.deleteAll();
        team = new Team();
        team.setTeamId(DEFAULT_TEAM_ID);
        team.setTeamName(DEFAULT_TEAM_NAME);
    }

    @Test
    @Transactional
    public void createTeam() throws Exception {
        int databaseSizeBeforeCreate = teamRepository.findAll().size();

        // Create the Team
        TeamDTO teamDTO = teamMapper.teamToTeamDTO(team);

        restTeamMockMvc.perform(post("/api/teams")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(teamDTO)))
                .andExpect(status().isCreated());

        // Validate the Team in the database
        List<Team> teams = teamRepository.findAll();
        assertThat(teams).hasSize(databaseSizeBeforeCreate + 1);
        Team testTeam = teams.get(teams.size() - 1);
        assertThat(testTeam.getTeamId()).isEqualTo(DEFAULT_TEAM_ID);
        assertThat(testTeam.getTeamName()).isEqualTo(DEFAULT_TEAM_NAME);

        // Validate the Team in ElasticSearch
        Team teamEs = teamSearchRepository.findOne(testTeam.getId());
        assertThat(teamEs).isEqualToComparingFieldByField(testTeam);
    }

    @Test
    @Transactional
    public void getAllTeams() throws Exception {
        // Initialize the database
        teamRepository.saveAndFlush(team);

        // Get all the teams
        restTeamMockMvc.perform(get("/api/teams?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(team.getId().intValue())))
                .andExpect(jsonPath("$.[*].teamId").value(hasItem(DEFAULT_TEAM_ID.intValue())))
                .andExpect(jsonPath("$.[*].teamName").value(hasItem(DEFAULT_TEAM_NAME.toString())));
    }

    @Test
    @Transactional
    public void getTeam() throws Exception {
        // Initialize the database
        teamRepository.saveAndFlush(team);

        // Get the team
        restTeamMockMvc.perform(get("/api/teams/{id}", team.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(team.getId().intValue()))
            .andExpect(jsonPath("$.teamId").value(DEFAULT_TEAM_ID.intValue()))
            .andExpect(jsonPath("$.teamName").value(DEFAULT_TEAM_NAME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingTeam() throws Exception {
        // Get the team
        restTeamMockMvc.perform(get("/api/teams/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateTeam() throws Exception {
        // Initialize the database
        teamRepository.saveAndFlush(team);
        teamSearchRepository.save(team);
        int databaseSizeBeforeUpdate = teamRepository.findAll().size();

        // Update the team
        Team updatedTeam = new Team();
        updatedTeam.setId(team.getId());
        updatedTeam.setTeamId(UPDATED_TEAM_ID);
        updatedTeam.setTeamName(UPDATED_TEAM_NAME);
        TeamDTO teamDTO = teamMapper.teamToTeamDTO(updatedTeam);

        restTeamMockMvc.perform(put("/api/teams")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(teamDTO)))
                .andExpect(status().isOk());

        // Validate the Team in the database
        List<Team> teams = teamRepository.findAll();
        assertThat(teams).hasSize(databaseSizeBeforeUpdate);
        Team testTeam = teams.get(teams.size() - 1);
        assertThat(testTeam.getTeamId()).isEqualTo(UPDATED_TEAM_ID);
        assertThat(testTeam.getTeamName()).isEqualTo(UPDATED_TEAM_NAME);

        // Validate the Team in ElasticSearch
        Team teamEs = teamSearchRepository.findOne(testTeam.getId());
        assertThat(teamEs).isEqualToComparingFieldByField(testTeam);
    }

    @Test
    @Transactional
    public void deleteTeam() throws Exception {
        // Initialize the database
        teamRepository.saveAndFlush(team);
        teamSearchRepository.save(team);
        int databaseSizeBeforeDelete = teamRepository.findAll().size();

        // Get the team
        restTeamMockMvc.perform(delete("/api/teams/{id}", team.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate ElasticSearch is empty
        boolean teamExistsInEs = teamSearchRepository.exists(team.getId());
        assertThat(teamExistsInEs).isFalse();

        // Validate the database is empty
        List<Team> teams = teamRepository.findAll();
        assertThat(teams).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchTeam() throws Exception {
        // Initialize the database
        teamRepository.saveAndFlush(team);
        teamSearchRepository.save(team);

        // Search the team
        restTeamMockMvc.perform(get("/api/_search/teams?query=id:" + team.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(team.getId().intValue())))
            .andExpect(jsonPath("$.[*].teamId").value(hasItem(DEFAULT_TEAM_ID.intValue())))
            .andExpect(jsonPath("$.[*].teamName").value(hasItem(DEFAULT_TEAM_NAME.toString())));
    }
}
