package com.advancedrace.plugin.command;

import com.advancedrace.plugin.AdvancedRace;
import com.advancedrace.plugin.listener.PlayerNameListener;
import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.GameStateManager;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.task.GameTimerTask;
import com.advancedrace.plugin.util.ScoreboardManager;
import com.advancedrace.plugin.util.ViewerInitializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class GameEndCommand implements CommandExecutor {

    private GameStateManager gameStateManager;
    private TeamManager teamManager;

    public GameEndCommand(GameStateManager gameStateManager, TeamManager teamManager) {
        this.gameStateManager = gameStateManager;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용 가능한 명령어입니다.");
            return false;
        }

        Player player = (Player) sender;

        // OP만 사용 가능
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "이 명령어는 OP만 사용 가능합니다.");
            return false;
        }

        AdvancedRace advancedRace = AdvancedRace.getInstance();

        // 게임 상태 확인 (진행 중이면 종료)
        if (gameStateManager.isRunning()) {
            // 승패 타이틀 표시
            displayWinnerAndLoser(advancedRace);
            gameStateManager.endGame();
        }

        // 게임 타이머 정지
        GameTimerTask timerTask = advancedRace.getGameTimerTask();
        if (timerTask != null) {
            timerTask.stop();
        }

        // 나침반 추적 정지
        advancedRace.getCompassTrackingManager().stop();

        // gamerule doImmediateRespawn 다시 false로 설정
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule doImmediateRespawn false");

        // Bukkit Scoreboard에서 모든 팀 제거
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team scoreboardTeam : mainScoreboard.getTeams()) {
            if (scoreboardTeam.getName().startsWith("team_")) {
                scoreboardTeam.unregister();
            }
        }

        // 모든 온라인 플레이어 초기화
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // 스코어보드 제거
            ScoreboardManager.removeScoreboard(onlinePlayer);

            // 모든 포션 효과 제거
            for (PotionEffect effect : onlinePlayer.getActivePotionEffects()) {
                onlinePlayer.removePotionEffect(effect.getType());
            }

            // 크기 원래대로 복구
            if (onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE) != null) {
                onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE).setBaseValue(1.0);
            }

            // 최대 체력 원래대로 복구
            if (onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null) {
                onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            }
            onlinePlayer.setHealth(20.0);

            // 게임모드를 SURVIVAL로 복구
            if (onlinePlayer.getGameMode() == GameMode.SPECTATOR) {
                onlinePlayer.setGameMode(GameMode.SURVIVAL);
            }

            // 인벤토리 클리어
            onlinePlayer.getInventory().clear();

            // 플레이어 디스플레이 네임 초기화 (탭리스트, 네임태그)
            onlinePlayer.playerListName(null);
            onlinePlayer.customName(null);
            onlinePlayer.setCustomNameVisible(false);
        }

        // 게임 데이터 삭제
        DataPersistence.deleteGameData();

        // 모든 팀 제거
        teamManager.removeAllTeams();

        // 모든 플레이어의 팀 변경 기회 초기화
        teamManager.clearAllDeathChances();

        // 모든 플레이어의 스폰 순위 초기화
        teamManager.clearAllSpawnTiers();

        // 모든 플레이어의 사망 횟수 초기화
        teamManager.clearAllDeathCounts();

        // 모든 소환된 시청자 정보 초기화
        teamManager.clearAllSummonedViewers();

        // 발전과제 팀 점수 초기화
        if (advancedRace.getAdvancementListener() != null) {
            advancedRace.getAdvancementListener().initializeTeamScores();
        }

        // 발전과제 완료 기록 초기화
        if (advancedRace.getAdvancementManager() != null) {
            advancedRace.getAdvancementManager().resetAdvancements();
        }

        // playerTeamMap 메모리 정리
        DataPersistence.clearPlayerTeamMap();

        player.sendMessage(ChatColor.GREEN + "✓ 게임이 종료되었습니다!");
        player.sendMessage(ChatColor.YELLOW + "모든 플레이어의 설정이 초기화되었습니다.");

        return true;
    }

    /**
     * 스트리머들의 최종 순위를 결정하고 승패 타이틀 표시
     */
    private void displayWinnerAndLoser(AdvancedRace advancedRace) {
        if (advancedRace == null) {
            return;
        }

        Map<String, Integer> teamScores = advancedRace.getAdvancementListener().getTeamScores();

        // 스트리머 정보와 점수를 저장할 리스트 생성
        List<Map.Entry<String, Integer>> scoredStreamers = new java.util.ArrayList<>(teamScores.entrySet());

        // 점수순으로 정렬하고, 동점일 경우 시청자 수로 정렬
        scoredStreamers.sort((a, b) -> {
            int scoreCompare = Integer.compare(b.getValue(), a.getValue()); // 내림차순
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            // 동점인 경우 시청자 수로 비교
            TeamManager.Team teamA = teamManager.getTeamByStreamer(a.getKey());
            TeamManager.Team teamB = teamManager.getTeamByStreamer(b.getKey());
            int viewersA = teamA != null ? teamA.getPlayerCount() : 0;
            int viewersB = teamB != null ? teamB.getPlayerCount() : 0;
            return Integer.compare(viewersB, viewersA); // 내림차순
        });

        // 우승팀 결정 (첫 번째가 우승팀)
        String winnerStreamer = scoredStreamers.isEmpty() ? null : scoredStreamers.get(0).getKey();

        // 스코어보드 로그 출력
        StringBuilder scoreLog = new StringBuilder("\n[AdvancedRace] 게임 종료 - 최종 순위:\n");
        for (int i = 0; i < scoredStreamers.size(); i++) {
            String streamerName = scoredStreamers.get(i).getKey();
            int score = scoredStreamers.get(i).getValue();
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            int viewers = team != null ? team.getPlayerCount() : 0;
            scoreLog.append((i + 1)).append("위: ").append(streamerName).append(" (점수: ").append(score)
                    .append(", 시청자: ").append(viewers).append(")\n");
        }
        Bukkit.getLogger().info(scoreLog.toString());

        // 각 스트리머에게 타이틀 표시 (승리/패배)
        for (String streamerName : teamManager.getStreamerNames()) {
            Player streamer = Bukkit.getPlayer(streamerName);
            if (streamer != null && streamer.isOnline()) {
                Component title;
                Component subtitle;

                if (streamerName.equals(winnerStreamer)) {
                    // 우승팀
                    title = Component.text("승리", NamedTextColor.GOLD);
                    subtitle = Component.empty();
                } else {
                    // 패배팀
                    title = Component.text("패배", NamedTextColor.GRAY);
                    subtitle = Component.empty();
                }

                streamer.showTitle(
                    Title.title(
                        title,
                        subtitle,
                        Title.Times.times(
                            Duration.ofMillis(500),  // fade in
                            Duration.ofMillis(3000), // stay
                            Duration.ofMillis(500)   // fade out
                        )
                    )
                );
            }
        }
    }
}
