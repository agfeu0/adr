package com.advancedrace.plugin.util;

import com.advancedrace.plugin.manager.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class ViewerInitializer {

    /**
     * 시청자 설정 초기화
     */
    public static void initializeViewer(Player player) {
        initializeViewer(player, null);
    }

    /**
     * 시청자 설정 초기화 (스폰 순위 포함)
     */
    public static void initializeViewer(Player player, TeamManager teamManager) {
        // 1. 인벤토리 5칸 제한 (베리어로 차단)
        setBarrierSlots(player);

        // 2. 체력 5칸(10)으로 고정
        setHalfHealth(player);

        // 3. 크기 0.7로 축소
        setScaleSmall(player);

        // 4. 5번 슬롯에 나침반 고정
        setCompassSlot5(player);

        // 5. 스폰 순위에 따른 지연 (후순위인 경우 5초 지연)
        if (teamManager != null && teamManager.getSpawnTier(player) == 2) {
            applySpawnDelay(player, teamManager);
        }
    }

    /**
     * 인벤토리를 5칸만 사용 가능하도록 베리어로 제한
     */
    private static void setBarrierSlots(Player player) {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c사용 불가");
            barrier.setItemMeta(meta);
        }

        // 5-35 슬롯에 베리어 배치
        for (int i = 5; i < 36; i++) {
            player.getInventory().setItem(i, barrier.clone());
        }
    }

    /**
     * 체력을 5칸(10)으로 고정
     */
    private static void setHalfHealth(Player player) {
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10.0);
        }
        player.setHealth(10.0);
    }

    /**
     * 플레이어 크기를 0.7로 축소
     */
    private static void setScaleSmall(Player player) {
        String command = "attribute " + player.getName() + " minecraft:scale base set 0.7";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * 5번 슬롯에 기본 나침반 설정
     */
    public static void setCompassSlot5(Player player) {
        ItemStack emptyCompass = new ItemStack(Material.COMPASS);
        player.getInventory().setItem(4, emptyCompass); // 슬롯 5 = 인덱스 4
    }

    /**
     * 팀장을 가리키는 나침반 생성
     */
    private static ItemStack createStreamerCompass(String streamerName) {
        Player streamer = Bukkit.getPlayer(streamerName);
        if (streamer == null) {
            return new ItemStack(Material.COMPASS);
        }

        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        if (meta == null) {
            return compass;
        }

        meta.setDisplayName("§b" + streamerName + " 팀장");
        meta.setLodestone(streamer.getLocation());
        meta.setLodestoneTracked(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        compass.setItemMeta(meta);
        return compass;
    }

    /**
     * 플레이어의 현재 팀의 스트리머를 가리키도록 나침반 업데이트
     */
    public static void updateCompass(Player player, String streamerName) {
        ItemStack compass = createStreamerCompass(streamerName);
        player.getInventory().setItem(4, compass);
    }

    /**
     * 플레이어에게 스폰 지연 적용 (5초)
     */
    private static void applySpawnDelay(Player player, TeamManager teamManager) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("AdvancedRace");
        if (plugin == null) {
            return;
        }

        // 5초 후 해제 (메시지 없음)
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // 5초 경과 후 특별한 처리 없음
        }, 100); // 100틱 = 5초
    }

    /**
     * 월드 이름을 한글로 변환
     */
    private static String getWorldNameKorean(String worldName) {
        return switch (worldName.toLowerCase()) {
            case "world" -> "오버월드";
            case "world_nether" -> "네더";
            case "world_the_end" -> "엔더";
            default -> worldName;
        };
    }

    /**
     * 플레이어에게 액션바로 스트리머 월드 정보 표시
     */
    public static void showStreamerWorldActionBar(Player player, Location streamerLocation) {
        if (streamerLocation == null) {
            return;
        }

        String worldName = getWorldNameKorean(streamerLocation.getWorld().getName());

        Component message = Component.text("팀장이 ", NamedTextColor.YELLOW)
                .append(Component.text(worldName, NamedTextColor.GOLD))
                .append(Component.text("에 있습니다", NamedTextColor.YELLOW));

        player.sendActionBar(message);
    }

}
