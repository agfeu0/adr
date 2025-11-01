package com.advancedrace.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class ViewerInitializer {

    /**
     * 시청자 설정 초기화
     */
    public static void initializeViewer(Player player) {
        // 1. 인벤토리 5칸 제한 (베리어로 차단)
        setBarrierSlots(player);

        // 2. 체력 5칸(10)으로 고정
        setHalfHealth(player);

        // 3. 크기 0.7로 축소
        setScaleSmall(player);

        // 4. 5번 슬롯에 나침반 고정
        setCompassSlot5(player);
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
     * 5번 슬롯에 나침반 설정
     */
    public static void setCompassSlot5(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        if (compass.getItemMeta() instanceof CompassMeta) {
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            meta.setDisplayName("§b팀장 위치");
            compass.setItemMeta(meta);
        }
        player.getInventory().setItem(4, compass); // 슬롯 5 = 인덱스 4
    }

    /**
     * 플레이어의 현재 팀의 스트리머를 가리키도록 나침반 업데이트
     */
    public static void updateCompass(Player player, String streamerName) {
        Player streamer = Bukkit.getPlayer(streamerName);
        if (streamer == null) {
            return;
        }

        ItemStack item = player.getInventory().getItem(4);
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        if (!(item.getItemMeta() instanceof CompassMeta)) {
            return;
        }

        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName("§b" + streamerName + " 팀장");
        meta.setLodestone(streamer.getLocation());
        meta.setLodestoneTracked(true);
        item.setItemMeta(meta);
    }
}
