package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class HardcoreDeathListener implements Listener {

    private TeamManager teamManager;
    private Map<String, BukkitTask> viewerFollowTasks = new HashMap<>();

    public HardcoreDeathListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();

        // OP나 스트리머는 처리 안함
        if (deadPlayer.isOp() || isStreamer(deadPlayer)) {
            return;
        }

        // 시청자인지 확인
        TeamManager.Team team = teamManager.getTeam(deadPlayer);
        if (team == null) {
            return;
        }

        // 사망 횟수 증가
        teamManager.incrementDeathCount(deadPlayer);
        int deathCount = teamManager.getDeathCount(deadPlayer);

        // 50% 확률로 네더의 별 드랍
        if (Math.random() < 0.5) {
            deadPlayer.getWorld().dropItem(deadPlayer.getLocation(), new ItemStack(Material.NETHER_STAR));
        }

        // 첫 번째 사망: 팀 변경 기회 부여
        if (deathCount == 1) {
            // 소환된 시청자 정보 제거 (다른 팀에서 중복 소환 방지)
            String streamerName = team.getStreamer();
            teamManager.removeSummonedViewer(streamerName, deadPlayer.getName());

            // 팀에서 제거 (팀 변경 가능하도록)
            teamManager.removePlayer(deadPlayer);

            // 팀 변경 기회 부여
            teamManager.grantDeathChance(deadPlayer);

            // 메시지
            deadPlayer.sendMessage(ChatColor.YELLOW + "1회 팀 변경이 가능합니다.");

            // 1초 후 스펙테이터 모드로 전환 및 스트리머 추적 시작
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                    Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                    () -> {
                        deadPlayer.setGameMode(GameMode.SPECTATOR);
                        teamManager.markAsSpectator(deadPlayer);
                        // 스트리머 추적 시작
                        startFollowingStreamer(deadPlayer, team);
                    },
                    20 // 1초 = 20틱
            );
        } else {
            // 두 번째 이상 사망: 팀에 남아있지만 대기 중 상태로
            // SpawnTier를 1로 설정 (대기 중, 발전과제로 다시 소환 가능)
            teamManager.setSpawnTier(deadPlayer, 1);

            // 1초 후 스펙테이터 모드로 전환 및 스트리머 추적 시작
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                    Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                    () -> {
                        deadPlayer.setGameMode(GameMode.SPECTATOR);
                        // 스트리머 추적 시작
                        startFollowingStreamer(deadPlayer, team);
                    },
                    20 // 1초 = 20틱
            );
        }
    }

    /**
     * 시청자가 스트리머를 따라다니도록 설정 (2칸 이상 못 벗어남)
     */
    private void startFollowingStreamer(Player viewer, TeamManager.Team team) {
        String playerName = viewer.getName();
        String streamerName = team.getStreamer();

        // 기존 추적 태스크 취소
        if (viewerFollowTasks.containsKey(playerName)) {
            viewerFollowTasks.get(playerName).cancel();
        }

        // 스트리머 추적 태스크 시작 (1틱마다)
        BukkitTask followTask = Bukkit.getScheduler().runTaskTimer(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (!viewer.isOnline() || viewer.getGameMode() != GameMode.SPECTATOR) {
                        // 온라인이 아니거나 스펙테이터가 아니면 태스크 취소
                        if (viewerFollowTasks.containsKey(playerName)) {
                            viewerFollowTasks.get(playerName).cancel();
                            viewerFollowTasks.remove(playerName);
                        }
                        return;
                    }

                    // 스트리머 찾기
                    Player streamer = Bukkit.getPlayer(streamerName);
                    if (streamer != null && streamer.isOnline()) {
                        double distance = viewer.getLocation().distance(streamer.getLocation());

                        // 2칸 이상 멀어지면 스트리머 위치로 텔레포트
                        if (distance > 2) {
                            viewer.teleport(streamer.getLocation());
                        }
                    } else {
                        // 스트리머가 오프라인이면 태스크 취소
                        if (viewerFollowTasks.containsKey(playerName)) {
                            viewerFollowTasks.get(playerName).cancel();
                            viewerFollowTasks.remove(playerName);
                        }
                    }
                },
                0L,  // 초기 딜레이 없음
                1L   // 매 틱마다 실행
        );

        viewerFollowTasks.put(playerName, followTask);
    }

    private boolean isStreamer(Player player) {
        for (String streamerName : teamManager.getStreamerNames()) {
            if (streamerName.equals(player.getName())) {
                return true;
            }
        }
        return false;
    }
}
