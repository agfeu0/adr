package com.advancedrace.plugin.manager;

import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {

    private Map<String, Team> teams = new HashMap<>();
    private Map<Player, String> playerTeams = new HashMap<>();
    private Map<String, String> streamerTeams = new HashMap<>(); // 스트리머 이름 -> 팀 이름 매핑

    /**
     * 새로운 팀 생성 (스트리머가 팀장)
     */
    public boolean createTeam(String streamerName) {
        if (streamerTeams.containsKey(streamerName)) {
            return false;
        }

        String teamName = "team_" + streamerName;
        Team team = new Team(teamName, streamerName);
        teams.put(teamName, team);
        streamerTeams.put(streamerName, teamName);

        return true;
    }

    /**
     * 플레이어를 팀에 추가
     */
    public boolean addPlayerToTeam(Player player, String streamerName) {
        String teamName = streamerTeams.get(streamerName);

        if (teamName == null || !teams.containsKey(teamName)) {
            return false;
        }

        // 이미 다른 팀에 속해있으면 제거
        if (playerTeams.containsKey(player)) {
            String oldTeam = playerTeams.get(player);
            teams.get(oldTeam).removePlayer(player);
        }

        teams.get(teamName).addPlayer(player);
        playerTeams.put(player, teamName);

        return true;
    }

    /**
     * 플레이어가 속한 팀 이름 반환
     */
    public String getPlayerTeam(Player player) {
        return playerTeams.get(player);
    }

    /**
     * 플레이어가 속한 팀 객체 반환
     */
    public Team getTeam(Player player) {
        String teamName = playerTeams.get(player);
        return teamName != null ? teams.get(teamName) : null;
    }

    /**
     * 팀 이름으로 팀 객체 반환
     */
    public Team getTeamByName(String teamName) {
        return teams.get(teamName);
    }

    /**
     * 스트리머 이름으로 팀 객체 반환
     */
    public Team getTeamByStreamer(String streamerName) {
        String teamName = streamerTeams.get(streamerName);
        return teamName != null ? teams.get(teamName) : null;
    }

    /**
     * 모든 팀의 스트리머 이름 리스트
     */
    public List<String> getStreamerNames() {
        return new ArrayList<>(streamerTeams.keySet());
    }

    /**
     * 팀 제거
     */
    public void removeTeam(String streamerName) {
        String teamName = streamerTeams.remove(streamerName);
        if (teamName != null) {
            teams.remove(teamName);
        }
    }

    /**
     * 플레이어 제거
     */
    public void removePlayer(Player player) {
        String teamName = playerTeams.remove(player);
        if (teamName != null && teams.containsKey(teamName)) {
            teams.get(teamName).removePlayer(player);
        }
    }

    public static class Team {
        private String name;
        private String streamer;
        private Set<Player> players = new HashSet<>();

        public Team(String name, String streamer) {
            this.name = name;
            this.streamer = streamer;
        }

        public void addPlayer(Player player) {
            players.add(player);
        }

        public void removePlayer(Player player) {
            players.remove(player);
        }

        public String getName() {
            return name;
        }

        public String getStreamer() {
            return streamer;
        }

        public Set<Player> getPlayers() {
            return new HashSet<>(players);
        }

        public int getPlayerCount() {
            return players.size();
        }
    }
}
