package com.socctrader.repository;

import com.socctrader.domain.FinancialAction;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the FinancialAction entity.
 */
@SuppressWarnings("unused")
public interface FinancialActionRepository extends JpaRepository<FinancialAction,Long> {

    @Query("select financialAction from FinancialAction financialAction where financialAction.user.login = ?#{principal.username}")
    List<FinancialAction> findByUserIsCurrentUser();

}
