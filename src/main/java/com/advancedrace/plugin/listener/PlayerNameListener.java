package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.AdvancedRace;
import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.util.ScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PlayerNameListener implements Listener {

    private TeamManager teamManager;

    public PlayerNameListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 게임 진행 중에 재입장하는 경우 처리
        boolean isStreamer = false;
        for (String streamerName : teamManager.getStreamerNames()) {
            if (streamerName.equals(player.getName())) {
                isStreamer = true;
                break;
            }
        }

        if (AdvancedRace.getInstance().getGameStateManager().isRunning()) {
            // 저장된 팀 정보 확인
            String streamerName = DataPersistence.getStreamerForPlayer(player.getName());
            int savedSpawnTier = 0;

            if (streamerName != null) {
                savedSpawnTier = DataPersistence.getPlayerSpawnTier(player.getName());
            }

            // 소환된 시청자(SpawnTier == 2)는 서바이벌로 입장
            if (savedSpawnTier == 2) {
                TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
                if (team != null) {
                    teamManager.addPlayerToTeam(player, streamerName);
                    teamManager.setSpawnTier(player, 2);
                    player.setGameMode(GameMode.SURVIVAL);
                }
            } else if (streamerName != null) {
                // 팀에 속한 관전자(죽은 시청자): 스펙테이터로 입장하고 스트리머에게 텔레포트
                TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
                if (team != null) {
                    teamManager.addPlayerToTeam(player, streamerName);
                    // SpawnTier를 1로 초기화 (스폰되지 않은 상태로 설정 - 스트리머 소환 대기)
                    teamManager.setSpawnTier(player, 1);
                    player.setGameMode(GameMode.SPECTATOR);

                    // 스트리머 위치로 텔레포트
                    Player streamer = Bukkit.getPlayer(streamerName);
                    if (streamer != null && streamer.isOnline()) {
                        player.teleport(streamer.getLocation());
                    }
                }
            } else if (!isStreamer) {
                // 팀에 속하지 않은 스펙테이터
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage("§e게임이 진행 중입니다. 스펙테이터 모드로 입장합니다.");
            }
        } else {
            // 게임이 진행 중이 아닐 때 팀 정보 복원
            String streamerName = DataPersistence.getStreamerForPlayer(player.getName());
            if (streamerName != null) {
                TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
                if (team != null) {
                    teamManager.addPlayerToTeam(player, streamerName);
                    // 저장된 SpawnTier 복원 (소환 상태 유지)
                    int savedSpawnTier = DataPersistence.getPlayerSpawnTier(player.getName());
                    teamManager.setSpawnTier(player, savedSpawnTier);
                }
            }
        }

        updatePlayerDisplay(player, teamManager);

        // 스코어보드 설정 (스트리머 또는 팀에 속한 모든 시청자)
        String playerStreamer = DataPersistence.getStreamerForPlayer(player.getName());
        if (playerStreamer == null || teamManager.getTeam(player) != null) {
            // playerStreamer == null = 스트리머 / teamManager.getTeam(player) != null = 팀에 속한 모든 시청자
            ScoreboardManager.setupScoreboard(player, teamManager);
        }

        // 팀에 속한 모든 플레이어의 시청자 수 업데이트 (게임 시작 전에만 - 1틱 지연 - 스코어보드 설정 완료 대기)
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                AdvancedRace.getInstance(),
                () -> {
                    // 게임이 시작되지 않았을 때만 업데이트 (게임 중에는 ViewerSummonManager에서 관리)
                    if (!AdvancedRace.getInstance().getGameStateManager().isRunning()) {
                        String playerTeamStreamer = DataPersistence.getStreamerForPlayer(player.getName());
                        if (playerTeamStreamer != null) {
                            TeamManager.Team playerTeam = teamManager.getTeamByStreamer(playerTeamStreamer);
                            if (playerTeam != null) {
                                // 팀의 플레이어 수는 이미 시청자만 포함 (스트리머는 팀에 속하지 않음)
                                int updatedViewerCount = playerTeam.getPlayerCount(); // 스트리머 제외 불필요
                                // 스트리머에게 시청자 수 업데이트
                                Player streamer = Bukkit.getPlayer(playerTeamStreamer);
                                if (streamer != null && streamer.isOnline()) {
                                    updateViewerCountDirect(streamer, updatedViewerCount);
                                }
                                // 팀의 모든 시청자에게도 업데이트
                                for (Player teamMember : playerTeam.getPlayers()) {
                                    if (!teamMember.getName().equals(playerTeamStreamer) && teamMember.isOnline()) {
                                        updateViewerCountDirect(teamMember, updatedViewerCount);
                                    }
                                }
                            }
                        }
                    }
                },
                1 // 1틱 지연
        );

        // 저장된 발전과제 점수 복원 및 남은 시간 업데이트 (1틱 지연)
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                AdvancedRace.getInstance(),
                () -> {
                    TeamManager.Team team = teamManager.getTeam(player);

                    // 팀이 없으면 스트리머인지 확인
                    if (team == null) {
                        team = getStreamerTeam(player, teamManager);
                    }

                    // 팀이 있으면 점수 복원
                    if (team != null) {
                        String teamStreamerName = team.getStreamer();
                        int savedScore = DataPersistence.loadTeamScores().getOrDefault(teamStreamerName, 0);
                        ScoreboardManager.updateScore(player, savedScore);
                    }

                    // 게임이 진행 중이면 현재 남은 시간 업데이트
                    AdvancedRace advancedRace = AdvancedRace.getInstance();
                    if (advancedRace.getGameStateManager().isRunning() && advancedRace.getGameTimerTask() != null) {
                        long remainingSeconds = advancedRace.getGameTimerTask().getRemainingSeconds();
                        long hours = remainingSeconds / 3600;
                        long minutes = (remainingSeconds % 3600) / 60;
                        long seconds = remainingSeconds % 60;
                        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                        ScoreboardManager.updateTime(player, timeString);
                    }
                },
                1
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Scoreboard 팀에서 플레이어 제거
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team scoreboardTeam : scoreboard.getTeams()) {
            if (scoreboardTeam.hasEntry(player.getName())) {
                scoreboardTeam.removeEntry(player.getName());
            }
        }
    }

    public static void updatePlayerDisplay(Player player, TeamManager teamManager) {
        TeamManager.Team team = teamManager.getTeam(player);
        boolean isStreamer = false;

        // 플레이어가 속한 팀이 없으면, 스트리머인지 확인
        if (team == null) {
            team = getStreamerTeam(player, teamManager);
            if (team != null) {
                isStreamer = true;
            }
        }

        if (team != null) {
            // Scoreboard 팀 설정으로 네임태그 색상 적용 (스트리머/시청자 모두)
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = "team_" + team.getStreamer();
            Team scoreboardTeam = scoreboard.getTeam(teamName);

            // 팀이 없으면 생성
            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.registerNewTeam(teamName);
            }

            // 색상 설정 (팀 생성 직후 바로 설정)
            scoreboardTeam.setColor(getChatColor(team.getColor()));

            // 플레이어를 팀에 추가
            if (!scoreboardTeam.hasEntry(player.getName())) {
                scoreboardTeam.addEntry(player.getName());
            }

            // 네임태그 색상 설정 (시청자만)
            if (!isStreamer) {
                Component customName = Component.text(player.getName(), TextColor.color(getColorValue(team.getColor())));
                player.customName(customName);
                player.setCustomNameVisible(true);
            }

            // 탭리스트: 스트리머는 닉네임만, 시청자는 [팀이름팀] 접두사
            if (isStreamer) {
                Component listName = Component.text(player.getName(), TextColor.color(getColorValue(team.getColor())));
                player.playerListName(listName);
            } else {
                Component listName = Component.text("[" + team.getStreamer() + "팀] ", TextColor.color(getColorValue(team.getColor())))
                        .append(Component.text(player.getName(), TextColor.color(getColorValue(team.getColor()))));
                player.playerListName(listName);
            }
        } else {
            // 팀이 없으면 기본값으로
            player.playerListName(Component.text(player.getName()));
            player.customName(null);
            player.setCustomNameVisible(false);

            // Scoreboard 팀에서 제거
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Team scoreboardTeam : scoreboard.getTeams()) {
                if (scoreboardTeam.hasEntry(player.getName())) {
                    scoreboardTeam.removeEntry(player.getName());
                }
            }
        }
    }

    /**
     * 플레이어가 스트리머인 경우 해당 팀 반환
     */
    private static TeamManager.Team getStreamerTeam(Player player, TeamManager teamManager) {
        for (String streamerName : teamManager.getStreamerNames()) {
            if (streamerName.equals(player.getName())) {
                return teamManager.getTeamByStreamer(streamerName);
            }
        }
        return null;
    }

    /**
     * 플레이어 스코어보드에 시청자 수를 직접 업데이트 (팀에 속한 모든 시청자)
     */
    private static void updateViewerCountDirect(Player player, int viewerCount) {
        org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();
        org.bukkit.scoreboard.Objective objective = scoreboard.getObjective("game");
        if (objective == null) return;

        // 기존 시청자 항목 제거
        scoreboard.getEntries().stream()
                .filter(entry -> entry.startsWith("§e시청자:"))
                .forEach(scoreboard::resetScores);

        // 새로운 시청자 수 추가
        objective.getScore("§e시청자: §f" + viewerCount).setScore(2);
    }

    private static int getColorValue(String colorCode) {
        return switch (colorCode) {
            case "§c" -> 0xFF5555; // 빨강
            case "§6" -> 0xFFAA00; // 주황
            case "§e" -> 0xFFFF55; // 노랑
            case "§2" -> 0x00AA00; // 초록
            case "§a" -> 0x55FF55; // 연두
            case "§1" -> 0x0000AA; // 파랑
            case "§b" -> 0x55FFFF; // 하늘
            case "§5" -> 0xAA00AA; // 보라
            case "§d" -> 0xFF55FF; // 분홍
            case "§7" -> 0xAAAAAA; // 회색
            case "§0" -> 0x000000; // 검정
            case "§f" -> 0xFFFFFF; // 하양
            default -> 0xFFFFFF;
        };
    }

    private static ChatColor getChatColor(String colorCode) {
        return switch (colorCode) {
            case "§c" -> ChatColor.RED;
            case "§6" -> ChatColor.GOLD;
            case "§e" -> ChatColor.YELLOW;
            case "§2" -> ChatColor.DARK_GREEN;
            case "§a" -> ChatColor.GREEN;
            case "§1" -> ChatColor.DARK_BLUE;
            case "§b" -> ChatColor.AQUA;
            case "§5" -> ChatColor.DARK_PURPLE;
            case "§d" -> ChatColor.LIGHT_PURPLE;
            case "§7" -> ChatColor.GRAY;
            case "§0" -> ChatColor.BLACK;
            case "§f" -> ChatColor.WHITE;
            default -> ChatColor.WHITE;
        };
    }
}
