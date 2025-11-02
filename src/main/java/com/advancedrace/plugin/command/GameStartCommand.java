package com.advancedrace.plugin.command;

import com.advancedrace.plugin.AdvancedRace;
import com.advancedrace.plugin.listener.PlayerNameListener;
import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.GameStateManager;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.task.GameTimerTask;
import com.advancedrace.plugin.util.SafeTeleporter;
import com.advancedrace.plugin.util.TablistManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class GameStartCommand implements CommandExecutor {

    private GameStateManager gameStateManager;
    private TeamManager teamManager;

    public GameStartCommand(GameStateManager gameStateManager, TeamManager teamManager) {
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

        // 이미 게임이 진행 중이면
        if (gameStateManager.isRunning()) {
            player.sendMessage(ChatColor.RED + "이미 게임이 진행 중입니다.");
            return false;
        }

        // 팀이 없으면
        if (teamManager.getStreamerNames().isEmpty()) {
            player.sendMessage(ChatColor.RED + "팀이 없습니다. 먼저 /스트리머 명령어로 팀을 만들어주세요.");
            return false;
        }

        // 게임 시작
        gameStateManager.startGame();

        // 발전과제 점수 초기화
        AdvancedRace advancedRace = AdvancedRace.getInstance();
        advancedRace.getAdvancementListener().initializeTeamScores();

        // 게임 데이터 저장 (초기화된 점수 및 게임 시간 포함)
        DataPersistence.saveGameData(teamManager, advancedRace.getAdvancementListener().getTeamScores(), advancedRace.getGameDurationSeconds());

        // 사망 횟수 초기화
        teamManager.clearAllDeathCounts();

        // 팀 변경 기회 초기화
        teamManager.clearAllDeathChances();

        // 모든 플레이어의 발전과제 초기화
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke @a everything");

        // 모든 온라인 플레이어의 인벤토리 초기화
        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // 인벤토리 초기화
            onlinePlayer.getInventory().clear();
        }

        // 모든 팀의 플레이어에 대해 SpawnTier 초기화
        for (String streamerName : teamManager.getStreamerNames()) {
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            if (team != null) {
                for (org.bukkit.entity.Player teamPlayer : team.getPlayers()) {
                    if (teamPlayer.isOnline()) {
                        teamManager.setSpawnTier(teamPlayer, 1);
                    }
                }
            }
        }

        player.sendMessage(ChatColor.GREEN + "✓ 게임 준비가 완료되었습니다!");
        player.sendMessage(ChatColor.YELLOW + "현재 팀: " + teamManager.getStreamerNames().size() + "개");

        // 5초 카운트다운
        Plugin plugin = Bukkit.getPluginManager().getPlugin("AdvancedRace");
        if (plugin != null) {
            // 5초
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "§l5초 뒤 게임 시작!");
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
            }, 0);

            // 4초
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "§l4초 뒤 게임 시작!");
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
            }, 20);

            // 3초
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "§l3초 뒤 게임 시작!");
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
            }, 40);

            // 2초
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Bukkit.broadcastMessage(ChatColor.GOLD + "§l2초 뒤 게임 시작!");
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.2f);
                }
            }, 60);

            // 1초
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Bukkit.broadcastMessage(ChatColor.RED + "§l1초 뒤 게임 시작!");
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
                }
            }, 80);

            // 게임 시작 (5초 후)
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Bukkit.broadcastMessage(ChatColor.GREEN + "§l게임 시작!");
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }

                // 나침반 추적 시작
                advancedRace.getCompassTrackingManager().start();

                // 탭 리스트 팀별 정렬 및 네임태그 색상 재설정 (3틱 지연 - Scoreboard 설정 완료 대기)
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    // 모든 온라인 플레이어의 네임태그 색상 강력하게 재설정
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        // 기존 Scoreboard 팀 제거
                        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                        for (org.bukkit.scoreboard.Team t : scoreboard.getTeams()) {
                            if (t.hasEntry(p.getName())) {
                                t.removeEntry(p.getName());
                            }
                        }
                        // 네임태그 색상 재설정
                        PlayerNameListener.updatePlayerDisplay(p, teamManager);
                    }
                    TablistManager.organizeTablist(teamManager);
                }, 3);

                int successCount = 0;
                int failCount = 0;

                // 스트리머들만 텔레포트
                for (String streamerName : teamManager.getStreamerNames()) {
                    Player streamer = Bukkit.getPlayer(streamerName);
                    if (streamer != null && streamer.isOnline()) {
                        if (SafeTeleporter.teleportToSafeLocation(streamer)) {
                            successCount++;
                            streamer.sendMessage(ChatColor.GREEN + "✓ 안전한 위치로 텔레포트되었습니다!");
                        } else {
                            failCount++;
                            streamer.sendMessage(ChatColor.RED + "✗ 안전한 위치를 찾을 수 없습니다.");
                        }
                    }
                }

                // 관리자에게 결과 전송
                player.sendMessage(ChatColor.YELLOW + "텔레포트 결과: 성공 " + successCount + "명, 실패 " + failCount + "명");

                // 게임 타이머 시작 (설정에서 가져온 시간)
                long gameDurationSeconds = advancedRace.getGameDurationSeconds();
                GameTimerTask timerTask = new GameTimerTask(gameStateManager, advancedRace, gameDurationSeconds);
                timerTask.start();
                advancedRace.setGameTimerTask(timerTask);

                long hours = gameDurationSeconds / 3600;
                long minutes = (gameDurationSeconds % 3600) / 60;
                player.sendMessage(ChatColor.AQUA + "✓ 게임 타이머가 시작되었습니다! (" + hours + "시간 " + minutes + "분)");
            }, 100); // 100틱 = 5초
        }

        return true;
    }
}
