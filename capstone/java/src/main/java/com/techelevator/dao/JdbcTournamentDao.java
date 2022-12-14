package com.techelevator.dao;

import com.techelevator.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class JdbcTournamentDao implements TournamentDao{

    private final JdbcTemplate jdbcTemplate;

    public JdbcTournamentDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    UserDao userDao;

    @Override
    public List<Tournament> findAllTournaments() {
        List<Tournament> tournaments = new ArrayList<>();
        String sql = "SELECT * FROM tournaments ORDER BY name";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
        while (results.next()) {
            Tournament tournament = mapRowToTournament(results);
            tournaments.add(tournament);
        }
        return tournaments;
    }

    @Override
    public List<Integer> getAllTournamentsByUserId(int id) {
        List<Integer> ids = new ArrayList<>();
        String sql = "Select tournament_id FROM tournament_user WHERE user_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, id);
        while (results.next()){
            ids.add(results.getInt("tournament_id"));
        }
        return ids;
    }

    @Override
    public Tournament findTournamentById(int tournamentId) {
        String sql = "SELECT * FROM tournaments WHERE tournament_id = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, tournamentId);
        if (results.next()) {
            return mapRowToTournament(results);
        } else {
            throw new TournamentNotFoundException();
        }
    }

    @Override
    public Tournament findByTournamentName(String tournamentName) {
        String sql = "SELECT * FROM tournaments WHERE name = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, tournamentName);
        if (results.next()) {
            return mapRowToTournament(results);
        } else {
            throw new TournamentNotFoundException();
        }
    }

    @Override
    public int findIdByTournamentName(String tournamentName) {
        if (tournamentName == null) throw new IllegalArgumentException("Tournament Name cannot be Null");
        int tournamentId;
        try {
            tournamentId = jdbcTemplate.queryForObject("SELECT tournament_id FROM tournaments WHERE name = ?", int.class, tournamentName);
        } catch (EmptyResultDataAccessException e) {
            throw new TournamentNotFoundException();
        }
        return tournamentId;
    }

    @Override
    public List<UserDTO> findUsersByTournamentId(int tournamentId) {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT * FROM users JOIN tournament_user ON users.user_id = tournament_user.user_id WHERE tournament_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql,tournamentId);
        while(results.next()) {
            UserDTO user = mapRowToUserDTO(results);
            users.add(user);
        }
        return users;
    }

    @Override
    public List<Match> findMatchesByTournamentId(int tournamentId) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE tournament_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql,tournamentId);
        while(results.next()) {
            Match match = mapRowToMatch(results);
            matches.add(match);
        }
        return matches;
    }

    @Override
    public int createTournament(Tournament tournament) {
        String sql = "INSERT INTO tournaments (organizer_id, name, num_of_participants, type, from_date, to_date, game, img_url) VALUES (?,?,?,?,?,?,?,?) RETURNING tournament_id";
        int newId = jdbcTemplate.queryForObject
                (sql, Integer.class, tournament.getOrganizerId(), tournament.getTournamentName(),
                        tournament.getNumOfParticipants(), tournament.getTournamentType(),
                        tournament.getFromDate(), tournament.getToDate(), tournament.getGame(), tournament.getImgUrl());
        return newId;
    }

    @Override
    public boolean updateTournament(Tournament tournament, int tournamentId) {
        String sql = "UPDATE tournaments SET organizer_id = ?, name = ?, num_of_participants = ?, type = ?, from_date = ?, to_date = ?, game = ?, img_url = ? WHERE tournament_id = ?";
        return jdbcTemplate.update
                (sql,tournament.getOrganizerId(), tournament.getTournamentName(),
                        tournament.getNumOfParticipants(), tournament.getTournamentType(),
                        tournament.getFromDate(), tournament.getToDate(), tournament.getGame(), tournament.getImgUrl(), tournamentId) == 1;
    }

    private Tournament mapRowToTournament(SqlRowSet rs) {
        Tournament tournament = new Tournament();
        tournament.setTournamentId(rs.getInt("tournament_id"));
        tournament.setOrganizerId(rs.getInt("organizer_id"));
        tournament.setTournamentName(rs.getString("name"));
        tournament.setNumOfParticipants(rs.getInt("num_of_participants"));
        tournament.setTournamentType(rs.getString("type"));
        tournament.setFromDate(rs.getDate("from_date"));
        tournament.setToDate(rs.getDate("to_date"));
        tournament.setImgUrl(rs.getString("img_url"));
        tournament.setGame(rs.getString("game"));
        User user = userDao.getUserById(tournament.getOrganizerId());
        String organizerName = user.getUsername();
        tournament.setOrganizerName(organizerName);
        return tournament;
    }
    private User mapRowToUser(SqlRowSet rs) {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password_hash"));
        user.setAuthorities(Objects.requireNonNull(rs.getString("role")));
        user.setActivated(true);
        return user;
    }

    private UserDTO mapRowToUserDTO(SqlRowSet rs) {
        UserDTO user = new UserDTO();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        return user;
    }

    private Match mapRowToMatch(SqlRowSet rs) {
        Match match = new Match();
        match.setMatchId(rs.getInt("match_id"));
        match.setTournamentId(rs.getInt("tournament_id"));
        match.setHomePlayer(rs.getString("home_player"));
        match.setAwayPlayer(rs.getString("away_player"));
        match.setWinner(rs.getString("winner"));
        return match;
    }
}
