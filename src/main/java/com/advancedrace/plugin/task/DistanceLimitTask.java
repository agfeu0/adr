package com.advancedrace.plugin.task;

import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.util.ViewerInitializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DistanceLimitTask extends BukkitRunnable {

    private TeamManager teamManager;
    private static final double MAX_DISTANCE = 25.0;

    public DistanceLimitTask(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // OP나 스트리머는 제한 없음
            if (player.isOp() || isStreamer(player)) {
                // 어둠 효과 제거
                player.removePotionEffect(PotionEffectType.DARKNESS);
                continue;
            }

            // 시청자인지 확인
            TeamManager.Team team = teamManager.getTeam(player);
            if (team == null) {
                continue;
            }

            // 소환된 시청자(SpawnTier == 2)에게만 거리 제한 적용
            if (teamManager.getSpawnTier(player) != 2) {
                // 소환되지 않은 플레이어는 어둠 효과 제거
                player.removePotionEffect(PotionEffectType.DARKNESS);
                continue;
            }

            // 스트리머 찾기
            String streamerName = team.getStreamer();
            Player streamer = Bukkit.getPlayer(streamerName);

            if (streamer == null || !streamer.isOnline()) {
                continue;
            }

            // 월드 확인 (다른 월드면 액션바에 메시지 표시)
            if (!player.getWorld().equals(streamer.getWorld())) {
                ViewerInitializer.showStreamerWorldActionBar(player, streamer.getLocation());
                player.removePotionEffect(PotionEffectType.DARKNESS);
                continue;
            }

            // 거리 계산 (같은 월드일 때만)
            double distance = player.getLocation().distance(streamer.getLocation());

            if (distance > MAX_DISTANCE) {
                // 25칸 이상 멀어지면 어둠 효과 적용 (무한 지속)
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, PotionEffect.INFINITE_DURATION, 1, false, false), true);
            } else {
                // 25칸 이내면 어둠 효과 제거
                player.removePotionEffect(PotionEffectType.DARKNESS);
            }
        }
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
