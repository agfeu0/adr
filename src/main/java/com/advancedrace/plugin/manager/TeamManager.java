package com.advancedrace.plugin.manager;

import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {

    private Map<String, Team> teams = new HashMap<>();
    private Map<Player, String> playerTeams = new HashMap<>();
    private Map<String, String> streamerTeams = new HashMap<>(); // 스트리머 이름 -> 팀 이름 매핑
    private Set<String> playersWithDeathChance = new HashSet<>(); // 1회 팀 변경 기회가 있는 플레이어
    private Set<String> spectatorsWithChance = new HashSet<>(); // 스펙테이터 모드인 플레이어 (팀 변경 기회 있음)
    private Map<String, Integer> playerSpawnTier = new HashMap<>(); // 플레이어 스폰 순위 (1 = 먼저, 2 = 나중)
    private Map<String, Integer> playerDeathCount = new HashMap<>(); // 플레이어 사망 횟수
    private Map<String, Set<String>> summonedViewers = new HashMap<>(); // 스트리머별 소환된 시청자 이름 목록

    /**
     * 새로운 팀 생성 (스트리머가 팀장)
     */
    public boolean createTeam(String streamerName) {
        return createTeam(streamerName, null);
    }

    /**
     * 새로운 팀 생성 (스트리머가 팀장, 색깔 지정)
     */
    public boolean createTeam(String streamerName, String color) {
        if (streamerTeams.containsKey(streamerName)) {
            return false;
        }

        String teamName = "team_" + streamerName;
        Team team = color != null ? new Team(teamName, streamerName, color) : new Team(teamName, streamerName);
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
     * 모든 팀 제거
     */
    public void removeAllTeams() {
        teams.clear();
        playerTeams.clear();
        streamerTeams.clear();
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

    /**
     * 플레이어에게 1회 팀 변경 기회 부여 (사망 시)
     */
    public void grantDeathChance(Player player) {
        playersWithDeathChance.add(player.getName());
    }

    /**
     * 플레이어가 팀 변경 기회가 있는지 확인
     */
    public boolean hasDeathChance(Player player) {
        return playersWithDeathChance.contains(player.getName());
    }

    /**
     * 플레이어의 팀 변경 기회 사용 (제거)
     */
    public void useDeathChance(Player player) {
        playersWithDeathChance.remove(player.getName());
    }

    /**
     * 모든 플레이어의 팀 변경 기회 초기화
     */
    public void clearAllDeathChances() {
        playersWithDeathChance.clear();
        spectatorsWithChance.clear();
    }

    /**
     * 플레이어를 스펙테이터로 등록 (팀 변경 기회 있음)
     */
    public void markAsSpectator(Player player) {
        spectatorsWithChance.add(player.getName());
    }

    /**
     * 플레이어가 스펙테이터 모드인지 확인
     */
    public boolean isSpectatorWithChance(Player player) {
        return spectatorsWithChance.contains(player.getName());
    }

    /**
     * 플레이어를 스펙테이터 목록에서 제거 (팀 변경 후)
     */
    public void removeFromSpectator(Player player) {
        spectatorsWithChance.remove(player.getName());
    }

    /**
     * 플레이어의 스폰 순위 설정
     * @param player 플레이어
     * @param tier 1 = 먼저 스폰, 2 = 나중에 스폰
     */
    public void setSpawnTier(Player player, int tier) {
        playerSpawnTier.put(player.getName(), tier);
    }

    /**
     * 플레이어의 스폰 순위 반환 (기본값: 1)
     */
    public int getSpawnTier(Player player) {
        return playerSpawnTier.getOrDefault(player.getName(), 1);
    }

    /**
     * 모든 플레이어의 스폰 순위 초기화
     */
    public void clearAllSpawnTiers() {
        playerSpawnTier.clear();
    }

    /**
     * 플레이어의 사망 횟수 증가
     */
    public void incrementDeathCount(Player player) {
        String playerName = player.getName();
        int currentCount = playerDeathCount.getOrDefault(playerName, 0);
        playerDeathCount.put(playerName, currentCount + 1);
    }

    /**
     * 플레이어의 사망 횟수 반환 (기본값: 0)
     */
    public int getDeathCount(Player player) {
        return playerDeathCount.getOrDefault(player.getName(), 0);
    }

    /**
     * 모든 플레이어의 사망 횟수 초기화
     */
    public void clearAllDeathCounts() {
        playerDeathCount.clear();
    }

    /**
     * 스트리머에게 시청자를 소환으로 표시
     */
    public void markViewerAsSummoned(String streamerName, String viewerName) {
        summonedViewers.computeIfAbsent(streamerName, k -> new HashSet<>()).add(viewerName);
    }

    /**
     * 스트리머의 소환된 시청자 수 반환 (팀에 속한 사람만 계산)
     */
    public int getSummonedViewerCount(String streamerName) {
        Team team = getTeamByStreamer(streamerName);
        if (team == null) {
            return 0;
        }

        Set<String> summoned = summonedViewers.getOrDefault(streamerName, new HashSet<>());
        int count = 0;
        for (Player player : team.getPlayers()) {
            if (summoned.contains(player.getName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 모든 소환된 시청자 정보 초기화 (게임 종료 시)
     */
    public void clearAllSummonedViewers() {
        summonedViewers.clear();
    }

    /**
     * 스트리머의 소환된 시청자 목록 반환
     */
    public Set<String> getSummonedViewersSet(String streamerName) {
        return new HashSet<>(summonedViewers.getOrDefault(streamerName, new HashSet<>()));
    }

    /**
     * 소환된 시청자 정보 일괄 설정 (데이터 로드 시)
     */
    public void setSummonedViewers(String streamerName, Set<String> viewers) {
        summonedViewers.put(streamerName, new HashSet<>(viewers));
    }

    public static class Team {
        private String name;
        private String streamer;
        private Set<Player> players = new HashSet<>();
        private String color;

        public Team(String name, String streamer) {
            this.name = name;
            this.streamer = streamer;
            this.color = getRandomColor();
        }

        public Team(String name, String streamer, String color) {
            this.name = name;
            this.streamer = streamer;
            this.color = color;
        }

        private static String getRandomColor() {
            String[] colors = {"§c", "§6", "§e", "§2", "§a", "§1", "§b", "§5", "§d", "§7", "§0", "§f"};
            return colors[(int) (Math.random() * colors.length)];
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

        public String getColor() {
            return color;
        }

        public Set<Player> getPlayers() {
            return new HashSet<>(players);
        }

        public int getPlayerCount() {
            return players.size();
        }
    }
}
