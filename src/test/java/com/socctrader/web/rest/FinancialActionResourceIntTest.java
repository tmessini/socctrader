package com.socctrader.web.rest;

import com.socctrader.SocctraderApp;
import com.socctrader.domain.FinancialAction;
import com.socctrader.repository.FinancialActionRepository;
import com.socctrader.repository.search.FinancialActionSearchRepository;

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

import com.socctrader.domain.enumeration.Action;
import com.socctrader.domain.enumeration.Currency;

/**
 * Test class for the FinancialActionResource REST controller.
 *
 * @see FinancialActionResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SocctraderApp.class)
@WebAppConfiguration
@IntegrationTest
public class FinancialActionResourceIntTest {


    private static final Long DEFAULT_ACTION_ID = 1L;
    private static final Long UPDATED_ACTION_ID = 2L;

    private static final Action DEFAULT_ACTION = Action.DEPOSIT;
    private static final Action UPDATED_ACTION = Action.WITHDRAW;

    private static final Double DEFAULT_AMOUNT = 1D;
    private static final Double UPDATED_AMOUNT = 2D;

    private static final Currency DEFAULT_CURRENCY = Currency.EUR;
    private static final Currency UPDATED_CURRENCY = Currency.USD;

    @Inject
    private FinancialActionRepository financialActionRepository;

    @Inject
    private FinancialActionSearchRepository financialActionSearchRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restFinancialActionMockMvc;

    private FinancialAction financialAction;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        FinancialActionResource financialActionResource = new FinancialActionResource();
        ReflectionTestUtils.setField(financialActionResource, "financialActionSearchRepository", financialActionSearchRepository);
        ReflectionTestUtils.setField(financialActionResource, "financialActionRepository", financialActionRepository);
        this.restFinancialActionMockMvc = MockMvcBuilders.standaloneSetup(financialActionResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        financialActionSearchRepository.deleteAll();
        financialAction = new FinancialAction();
        financialAction.setActionId(DEFAULT_ACTION_ID);
        financialAction.setAction(DEFAULT_ACTION);
        financialAction.setAmount(DEFAULT_AMOUNT);
        financialAction.setCurrency(DEFAULT_CURRENCY);
    }

    @Test
    @Transactional
    public void createFinancialAction() throws Exception {
        int databaseSizeBeforeCreate = financialActionRepository.findAll().size();

        // Create the FinancialAction

        restFinancialActionMockMvc.perform(post("/api/financial-actions")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(financialAction)))
                .andExpect(status().isCreated());

        // Validate the FinancialAction in the database
        List<FinancialAction> financialActions = financialActionRepository.findAll();
        assertThat(financialActions).hasSize(databaseSizeBeforeCreate + 1);
        FinancialAction testFinancialAction = financialActions.get(financialActions.size() - 1);
        assertThat(testFinancialAction.getActionId()).isEqualTo(DEFAULT_ACTION_ID);
        assertThat(testFinancialAction.getAction()).isEqualTo(DEFAULT_ACTION);
        assertThat(testFinancialAction.getAmount()).isEqualTo(DEFAULT_AMOUNT);
        assertThat(testFinancialAction.getCurrency()).isEqualTo(DEFAULT_CURRENCY);

        // Validate the FinancialAction in ElasticSearch
        FinancialAction financialActionEs = financialActionSearchRepository.findOne(testFinancialAction.getId());
        assertThat(financialActionEs).isEqualToComparingFieldByField(testFinancialAction);
    }

    @Test
    @Transactional
    public void getAllFinancialActions() throws Exception {
        // Initialize the database
        financialActionRepository.saveAndFlush(financialAction);

        // Get all the financialActions
        restFinancialActionMockMvc.perform(get("/api/financial-actions?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(financialAction.getId().intValue())))
                .andExpect(jsonPath("$.[*].actionId").value(hasItem(DEFAULT_ACTION_ID.intValue())))
                .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
                .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.doubleValue())))
                .andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY.toString())));
    }

    @Test
    @Transactional
    public void getFinancialAction() throws Exception {
        // Initialize the database
        financialActionRepository.saveAndFlush(financialAction);

        // Get the financialAction
        restFinancialActionMockMvc.perform(get("/api/financial-actions/{id}", financialAction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(financialAction.getId().intValue()))
            .andExpect(jsonPath("$.actionId").value(DEFAULT_ACTION_ID.intValue()))
            .andExpect(jsonPath("$.action").value(DEFAULT_ACTION.toString()))
            .andExpect(jsonPath("$.amount").value(DEFAULT_AMOUNT.doubleValue()))
            .andExpect(jsonPath("$.currency").value(DEFAULT_CURRENCY.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingFinancialAction() throws Exception {
        // Get the financialAction
        restFinancialActionMockMvc.perform(get("/api/financial-actions/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateFinancialAction() throws Exception {
        // Initialize the database
        financialActionRepository.saveAndFlush(financialAction);
        financialActionSearchRepository.save(financialAction);
        int databaseSizeBeforeUpdate = financialActionRepository.findAll().size();

        // Update the financialAction
        FinancialAction updatedFinancialAction = new FinancialAction();
        updatedFinancialAction.setId(financialAction.getId());
        updatedFinancialAction.setActionId(UPDATED_ACTION_ID);
        updatedFinancialAction.setAction(UPDATED_ACTION);
        updatedFinancialAction.setAmount(UPDATED_AMOUNT);
        updatedFinancialAction.setCurrency(UPDATED_CURRENCY);

        restFinancialActionMockMvc.perform(put("/api/financial-actions")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedFinancialAction)))
                .andExpect(status().isOk());

        // Validate the FinancialAction in the database
        List<FinancialAction> financialActions = financialActionRepository.findAll();
        assertThat(financialActions).hasSize(databaseSizeBeforeUpdate);
        FinancialAction testFinancialAction = financialActions.get(financialActions.size() - 1);
        assertThat(testFinancialAction.getActionId()).isEqualTo(UPDATED_ACTION_ID);
        assertThat(testFinancialAction.getAction()).isEqualTo(UPDATED_ACTION);
        assertThat(testFinancialAction.getAmount()).isEqualTo(UPDATED_AMOUNT);
        assertThat(testFinancialAction.getCurrency()).isEqualTo(UPDATED_CURRENCY);

        // Validate the FinancialAction in ElasticSearch
        FinancialAction financialActionEs = financialActionSearchRepository.findOne(testFinancialAction.getId());
        assertThat(financialActionEs).isEqualToComparingFieldByField(testFinancialAction);
    }

    @Test
    @Transactional
    public void deleteFinancialAction() throws Exception {
        // Initialize the database
        financialActionRepository.saveAndFlush(financialAction);
        financialActionSearchRepository.save(financialAction);
        int databaseSizeBeforeDelete = financialActionRepository.findAll().size();

        // Get the financialAction
        restFinancialActionMockMvc.perform(delete("/api/financial-actions/{id}", financialAction.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate ElasticSearch is empty
        boolean financialActionExistsInEs = financialActionSearchRepository.exists(financialAction.getId());
        assertThat(financialActionExistsInEs).isFalse();

        // Validate the database is empty
        List<FinancialAction> financialActions = financialActionRepository.findAll();
        assertThat(financialActions).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchFinancialAction() throws Exception {
        // Initialize the database
        financialActionRepository.saveAndFlush(financialAction);
        financialActionSearchRepository.save(financialAction);

        // Search the financialAction
        restFinancialActionMockMvc.perform(get("/api/_search/financial-actions?query=id:" + financialAction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(financialAction.getId().intValue())))
            .andExpect(jsonPath("$.[*].actionId").value(hasItem(DEFAULT_ACTION_ID.intValue())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.doubleValue())))
            .andExpect(jsonPath("$.[*].currency").value(hasItem(DEFAULT_CURRENCY.toString())));
    }
}
