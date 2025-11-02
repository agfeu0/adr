package com.advancedrace.plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CompassTrackingManager {

    private final Plugin plugin;
    private final TeamManager teamManager;
    private BukkitRunnable task;

    public CompassTrackingManager(Plugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void start() {
        if (this.task != null) {
            this.task.cancel();
        }

        this.task = new BukkitRunnable() {
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // 팀에 속한 플레이어만 처리
                    TeamManager.Team team = teamManager.getTeam(player);
                    if (team == null) {
                        continue;
                    }

                    // 팀장 정보 조회
                    String streamerName = team.getStreamer();
                    Player streamer = Bukkit.getPlayer(streamerName);

                    if (streamer == null || !streamer.isOnline()) {
                        continue;
                    }

                    // 나침반 찾기
                    ItemStack compass = null;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.COMPASS) {
                            compass = item;
                            break;
                        }
                    }

                    if (compass == null) {
                        continue;
                    }

                    // 나침반 메타 업데이트
                    ItemMeta meta = compass.getItemMeta();
                    if (meta == null) {
                        continue;
                    }

                    // 나침반이 팀장을 가리키도록 설정
                    player.setCompassTarget(streamer.getLocation());
                    meta.setDisplayName("§b" + streamerName + " 팀장");
                    compass.setItemMeta(meta);

                    // 다른 월드에 있으면 액션바 표시
                    if (!player.getWorld().getName().equals(streamer.getWorld().getName())) {
                        String worldName = getWorldNameKorean(streamer.getWorld().getName());
                        player.sendActionBar("§e팀장이 " + worldName + "에 있습니다");
                    }
                }
            }
        };
        this.task.runTaskTimer(this.plugin, 0L, 20L); // 20틱 = 1초
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * 월드 이름을 한글로 변환
     */
    private String getWorldNameKorean(String worldName) {
        return switch (worldName.toLowerCase()) {
            case "world" -> "오버월드";
            case "world_nether" -> "네더";
            case "world_the_end" -> "엔더";
            default -> worldName;
        };
    }
}
