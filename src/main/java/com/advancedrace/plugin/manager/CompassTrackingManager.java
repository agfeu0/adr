package com.advancedrace.plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class CompassTrackingManager {

    private final Plugin plugin;
    private final TeamManager teamManager;
    private BukkitRunnable task;
    private final Map<String, Location> lastOverworldLocation = new HashMap<>(); // 스트리머별 마지막 오버월드 위치

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
                    CompassMeta meta = (CompassMeta) compass.getItemMeta();
                    if (meta == null) {
                        continue;
                    }

                    // 스트리머가 오버월드에 있으면 위치 업데이트, 아니면 마지막 오버월드 위치 유지
                    if (streamer.getWorld().getName().equals("world")) {
                        // 오버월드: 현재 위치 저장 및 나침반 업데이트
                        lastOverworldLocation.put(streamerName, streamer.getLocation());
                        meta.setLodestone(streamer.getLocation());
                    } else {
                        // 다른 월드: 마지막 오버월드 위치로 나침반 설정
                        Location lastLocation = lastOverworldLocation.get(streamerName);
                        if (lastLocation != null) {
                            meta.setLodestone(lastLocation);
                        } else {
                            // 마지막 위치가 없으면 현재 위치 사용
                            meta.setLodestone(streamer.getLocation());
                        }
                    }

                    meta.setLodestoneTracked(false); // 블록 없이도 방향만 가리킴
                    meta.setDisplayName("§b스트리머 " + streamerName);
                    compass.setItemMeta(meta);

                    // 다른 월드에 있으면 액션바 표시 (월드별 색깔 다르게)
                    if (!player.getWorld().getName().equals(streamer.getWorld().getName())) {
                        String worldName = getWorldNameKorean(streamer.getWorld().getName());
                        String coloredWorldName = getColoredWorldName(streamer.getWorld().getName());
                        player.sendActionBar("§e스트리머가 " + coloredWorldName + "§e에 있습니다");
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

    /**
     * 월드별 색깔이 적용된 이름 반환
     */
    private String getColoredWorldName(String worldName) {
        return switch (worldName.toLowerCase()) {
            case "world" -> "§a오버월드";
            case "world_nether" -> "§c네더";
            case "world_the_end" -> "§d엔더";
            default -> "§f" + worldName;
        };
    }
}
