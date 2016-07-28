package com.socctrader.web.rest.mapper;

import com.socctrader.domain.*;
import com.socctrader.web.rest.dto.TeamDTO;

import org.mapstruct.*;
import java.util.List;

/**
 * Mapper for the entity Team and its DTO TeamDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface TeamMapper {

    @Mapping(source = "league.id", target = "leagueId")
    TeamDTO teamToTeamDTO(Team team);

    List<TeamDTO> teamsToTeamDTOs(List<Team> teams);

    @Mapping(source = "leagueId", target = "league")
    Team teamDTOToTeam(TeamDTO teamDTO);

    List<Team> teamDTOsToTeams(List<TeamDTO> teamDTOs);

    default League leagueFromId(Long id) {
        if (id == null) {
            return null;
        }
        League league = new League();
        league.setId(id);
        return league;
    }
}
