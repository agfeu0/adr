package com.advancedrace.plugin.listener;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StreamerDeathListener implements Listener {

    private TeamManager teamManager;
    private Random random = new Random();

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
        } else {
            // 시청자가 없으면 스트리머에게 3분간 행동 불가 효과
            applyInactionEffect(deadPlayer);
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
     * 스트리머에게 3분간 행동 불가 효과 적용
     */
    private void applyInactionEffect(Player streamer) {
        // 3분 = 3600틱
        int duration = 3600;

        // Mining Fatigue 255 (채광 불가)
        streamer.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 254, false, false), true);

        // Slowness 255 (이동 불가)
        streamer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 254, false, false), true);

        // Jump Boost 255 (점프 불가 - 점프 높이 제거)
        streamer.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 254, false, false), true);

        // 메시지
        Bukkit.broadcastMessage(ChatColor.RED + streamer.getName() + "님의 팀에 시청자가 없어서 3분간 행동 불가 상태가 되었습니다.");
        streamer.sendMessage(ChatColor.YELLOW + "3분 후 행동 가능!");

        // 3분 후 메시지
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                Bukkit.getPluginManager().getPlugin("AdvancedRace"),
                () -> {
                    if (streamer.isOnline()) {
                        streamer.sendMessage(ChatColor.GREEN + "✓ 행동 불가 상태가 해제되었습니다!");
                    }
                },
                duration
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
