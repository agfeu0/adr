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
    private Map<String, BukkitTask> streamerFollowTasks = new HashMap<>();

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
            // 시청자가 없으면 스트리머를 3분간 관전 모드로
            applySpectatorMode(deadPlayer);
        }

        // 스트리머 즉시 체력 회복 (최대 체력)
        deadPlayer.setHealth(deadPlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        deadPlayer.sendMessage(ChatColor.GREEN + "✓ 체력이 회복되었습니다!");
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
     * 스트리머를 3분간 관전 모드로 설정 (팀 스트리머에게 무한 텔레포트, 움직임 불가)
     */
    private void applySpectatorMode(Player streamer) {
        String streamerName = streamer.getName();
        TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);

        // 관전 모드 전환
        streamer.setGameMode(GameMode.SPECTATOR);
        streamer.removePotionEffect(PotionEffectType.DARKNESS);

        // 메시지
        Bukkit.broadcastMessage(ChatColor.RED + streamer.getName() + "님의 팀에 시청자가 없어서 3분간 관전 모드가 되었습니다.");
        streamer.sendMessage(ChatColor.YELLOW + "팀 스트리머에게 고정되어 움직일 수 없습니다.");
        streamer.sendMessage(ChatColor.YELLOW + "3분 후 서바이벌 모드로 복구됩니다!");

        // 팀 스트리머에게 무한 텔레포트 태스크 시작 (1틱마다)
        BukkitTask followTask = Bukkit.getScheduler().runTaskTimer(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (!streamer.isOnline() || streamer.getGameMode() != GameMode.SPECTATOR) {
                        // 스트리머가 오프라인이거나 스펙테이터가 아니면 태스크 취소
                        if (streamerFollowTasks.containsKey(streamerName)) {
                            streamerFollowTasks.get(streamerName).cancel();
                            streamerFollowTasks.remove(streamerName);
                        }
                        return;
                    }

                    // 팀 스트리머 찾기
                    if (team != null) {
                        Player teamStreamer = Bukkit.getPlayer(team.getStreamer());
                        if (teamStreamer != null && teamStreamer.isOnline()) {
                            // 팀 스트리머의 위치로 텔레포트
                            streamer.teleport(teamStreamer.getLocation());
                        }
                    }
                },
                0L,  // 초기 딜레이 없음
                1L   // 매 틱마다 실행 (무한 텔레포트)
        );
        streamerFollowTasks.put(streamerName, followTask);

        // 3분 후 (3600틱) 서바이벌 모드로 복구
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (streamer.isOnline()) {
                        // 서바이벌 모드로 복구
                        streamer.setGameMode(GameMode.SURVIVAL);

                        // 저항 1 적용 (10초 = 200틱)
                        streamer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0, false, false), true);

                        streamer.sendMessage(ChatColor.GREEN + "✓ 서바이벌 모드로 복구되었습니다!");
                        streamer.sendMessage(ChatColor.YELLOW + "10초간 저항 1 상태입니다.");
                    }

                    // 팔로우 태스크 취소
                    if (streamerFollowTasks.containsKey(streamerName)) {
                        streamerFollowTasks.get(streamerName).cancel();
                        streamerFollowTasks.remove(streamerName);
                    }
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
