package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        streamer.sendMessage(ChatColor.YELLOW + "3분간 움직일 수 없습니다.");
        streamer.sendMessage(ChatColor.YELLOW + "3분 후 행동 불가 상태가 해제됩니다!");

        // 움직임 방지 태스크 시작 (매 틱마다 원래 위치로 텔레포트)
        BukkitTask immobilizeTask = Bukkit.getScheduler().runTaskTimer(
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

                    Location deathLoc = streamerDeathLocations.get(streamerName);
                    if (deathLoc != null) {
                        // 원래 사망 위치로 매 틱마다 텔레포트 (움직임 불가)
                        streamer.teleport(deathLoc);
                    }
                },
                0L, // 초기 딜레이 없음
                1L  // 매 틱마다 실행
        );
        streamerBoundaryTasks.put(streamerName, immobilizeTask);

        // 액션바 타이머 시작 (1초마다 업데이트, 20틱)
        final int[] remainingSeconds = {180}; // 3분 = 180초
        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (!streamer.isOnline() || streamer.getGameMode() != GameMode.SPECTATOR) {
                        // 스트리머가 오프라인이거나 스펙테이터가 아니면 태스크 취소
                        if (streamerBoundaryTasks.containsKey(streamerName + "_actionbar")) {
                            BukkitTask task = streamerBoundaryTasks.get(streamerName + "_actionbar");
                            task.cancel();
                            streamerBoundaryTasks.remove(streamerName + "_actionbar");
                        }
                        return;
                    }

                    // 분과 초 계산
                    int minutes = remainingSeconds[0] / 60;
                    int seconds = remainingSeconds[0] % 60;
                    String timeString = String.format("%d:%02d", minutes, seconds);

                    // 액션바에 시간 표시
                    Component actionBar = Component.text("행동 불가 상태 - ", NamedTextColor.RED)
                            .append(Component.text(timeString, NamedTextColor.YELLOW));
                    streamer.sendActionBar(actionBar);

                    remainingSeconds[0]--;
                },
                0L, // 초기 딜레이 없음
                20L  // 1초마다 (20틱)
        );
        streamerBoundaryTasks.put(streamerName + "_actionbar", actionBarTask);

        // 3분 후 (3600틱) 서바이벌 모드로 복구
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (streamer.isOnline()) {
                        // 서바이벌 모드로 복구
                        streamer.setGameMode(GameMode.SURVIVAL);

                        // 저항 5 적용 (10초 = 200틱)
                        streamer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4, false, false), true);

                        streamer.sendMessage(ChatColor.GREEN + "✓ 행동 불가 상태가 해제되었습니다!");
                        streamer.sendMessage(ChatColor.YELLOW + "10초간 저항 5 상태입니다.");
                    }

                    // 움직임 방지 태스크 취소
                    if (streamerBoundaryTasks.containsKey(streamerName)) {
                        streamerBoundaryTasks.get(streamerName).cancel();
                        streamerBoundaryTasks.remove(streamerName);
                    }

                    // 액션바 타이머 태스크 취소
                    if (streamerBoundaryTasks.containsKey(streamerName + "_actionbar")) {
                        streamerBoundaryTasks.get(streamerName + "_actionbar").cancel();
                        streamerBoundaryTasks.remove(streamerName + "_actionbar");
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
