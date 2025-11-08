package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StreamerDeathListener implements Listener {

    private TeamManager teamManager;
    private Random random = new Random();
    private Map<String, Location> streamerDeathLocations = new HashMap<>();
    private Map<String, BukkitTask> streamerBoundaryTasks = new HashMap<>();

    public StreamerDeathListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onStreamerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();

        // 죽은 플레이어가 스트리머인지 확인
        if (!isStreamer(deadPlayer)) {
            return;
        }

        String streamerName = deadPlayer.getName();
        TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);

        if (team == null) {
            return;
        }

        // 사망 캔슬
        event.setCancelled(true);

        // 팀의 소환된 시청자 목록만 (스트리머 제외, 소환된 시청자만)
        List<Player> viewers = new ArrayList<>();
        for (Player player : team.getPlayers()) {
            // 소환된 시청자(SpawnTier == 2)만 희생 대상
            if (!player.equals(deadPlayer) && !isStreamer(player) &&
                teamManager.getSpawnTier(player) == 2) {
                viewers.add(player);
            }
        }

        if (!viewers.isEmpty()) {
            // 시청자가 있으면 무작위로 1명 선택해서 희생
            Player sacrificed = viewers.get(random.nextInt(viewers.size()));
            sacrificeViewer(sacrificed, streamerName);
            // 스트리머의 모든 포션 효과 제거 (혹시 있을 수 있는 영향 제거)
            deadPlayer.getActivePotionEffects().forEach(effect -> deadPlayer.removePotionEffect(effect.getType()));
        } else {
            // 시청자가 없으면 스트리머를 3분간 관전 모드로 (5칸 범위 제한)
            applySpectatorMode(deadPlayer);
        }
    }

    /**
     * 시청자를 희생 (처치)
     */
    private void sacrificeViewer(Player viewer, String streamerName) {
        // 플레이어를 처치
        viewer.setHealth(0);

        // 메시지 전송
        Bukkit.broadcastMessage(ChatColor.RED + streamerName + "님의 팀의 " + viewer.getName() + "님이 희생되었습니다!");
    }

    /**
     * 스트리머를 3분간 관전 모드로 설정 (5칸 범위 제한)
     */
    private void applySpectatorMode(Player streamer) {
        String streamerName = streamer.getName();

        // 사망 위치 저장
        Location deathLocation = streamer.getLocation();
        streamerDeathLocations.put(streamerName, deathLocation.clone());

        // 관전 모드 전환
        streamer.setGameMode(GameMode.SPECTATOR);
        streamer.removePotionEffect(PotionEffectType.DARKNESS);

        // 메시지
        Bukkit.broadcastMessage(ChatColor.RED + streamer.getName() + "님의 팀에 시청자가 없어서 3분간 관전 모드가 되었습니다.");
        streamer.sendMessage(ChatColor.YELLOW + "죽은 위치에서 5칸 범위를 벗어나면 자동으로 돌아옵니다.");
        streamer.sendMessage(ChatColor.YELLOW + "3분 후 서바이벌 모드로 복구됩니다!");

        // 경계 체크 태스크 시작 (3초마다, 60틱)
        BukkitTask boundaryTask = Bukkit.getScheduler().runTaskTimer(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (!streamer.isOnline() || streamer.getGameMode() != GameMode.SPECTATOR) {
                        // 스트리머가 오프라인이거나 스펙테이터가 아니면 태스크 취소
                        if (streamerBoundaryTasks.containsKey(streamerName)) {
                            streamerBoundaryTasks.get(streamerName).cancel();
                            streamerBoundaryTasks.remove(streamerName);
                        }
                        return;
                    }

                    Location currentLocation = streamer.getLocation();
                    Location deathLoc = streamerDeathLocations.get(streamerName);

                    if (deathLoc != null && currentLocation.distance(deathLoc) > 5) {
                        // 5칸을 벗어났으면 사망 위치로 텔레포트
                        streamer.teleport(deathLoc);
                        streamer.sendMessage(ChatColor.RED + "죽은 위치에서 5칸을 벗어났습니다. 되돌려집니다.");
                    }
                },
                60L, // 초기 딜레이
                60L  // 반복 간격 (3초)
        );
        streamerBoundaryTasks.put(streamerName, boundaryTask);

        // 3분 후 (3600틱) 서바이벌 모드로 복구
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (streamer.isOnline()) {
                        // 서바이벌 모드로 복구
                        streamer.setGameMode(GameMode.SURVIVAL);

                        // 저항 1 적용 (10초 = 200틱)
                        streamer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0, false, false), true);

                        streamer.sendMessage(ChatColor.GREEN + "✓ 행동 불가 상태가 해제되었습니다!");
                        streamer.sendMessage(ChatColor.YELLOW + "10초간 저항 1 상태입니다.");
                    }

                    // 경계 체크 태스크 취소
                    if (streamerBoundaryTasks.containsKey(streamerName)) {
                        streamerBoundaryTasks.get(streamerName).cancel();
                        streamerBoundaryTasks.remove(streamerName);
                    }

                    // 사망 위치 정보 제거
                    streamerDeathLocations.remove(streamerName);
                },
                3600 // 3분 = 3600틱
        );
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
