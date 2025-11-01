package com.advancedrace.plugin.util;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ViewerInitializer {

    /**
     * 시청자 설정 초기화
     */
    public static void initializeViewer(Player player) {
        // 1. 인벤토리 5칸 제한
        player.getInventory().setMaxStackSize(1); // 임시 방지
        clearInventoryExcept5Slots(player);

        // 2. 체력 절반(10)으로 고정
        setHalfHealth(player);

        // 3. 크기 0.7로 축소
        setScaleSmall(player);

        // 4. 5번 슬롯에 나침반 고정
        setCompassSlot5(player);

        // 5. 인벤토리 크기 제한 (5칸)
        restrictInventorySize(player);
    }

    /**
     * 인벤토리를 5칸만 사용 가능하도록 제한
     */
    private static void clearInventoryExcept5Slots(Player player) {
        // 0~4번 슬롯만 유지, 5~35번 슬롯 비우기 (총 36칸의 인벤토리)
        for (int i = 5; i < 36; i++) {
            player.getInventory().setItem(i, null);
        }
        // 갑옷 슬롯 비우기
        player.getInventory().setArmorContents(null);
        // 오프손 비우기
        player.getInventory().setItemInOffHand(null);
    }

    /**
     * 체력을 절반(10)으로 고정
     */
    private static void setHalfHealth(Player player) {
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
        }
        player.setHealth(10.0);
    }

    /**
     * 플레이어 크기를 0.7로 축소
     * Paper 1.21.1에서는 직접 지원하지 않으므로 추후 구현 예정
     */
    private static void setScaleSmall(Player player) {
        // Paper 1.21.1에서는 scale 직접 설정이 없음
        // 추후 데이터팩이나 다른 방식으로 구현 필요
    }

    /**
     * 5번 슬롯에 나침반 설정
     */
    public static void setCompassSlot5(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b팀장 위치");
            compass.setItemMeta(meta);
        }
        player.getInventory().setItem(4, compass); // 슬롯 5 = 인덱스 4
    }

    /**
     * 인벤토리 크기를 5칸으로 제한
     */
    private static void restrictInventorySize(Player player) {
        // 플레이어의 인벤토리를 5칸으로 제한하려면
        // 주기적으로 5칸 이상의 아이템을 지워야 함
        // 이는 Task로 처리하는 것이 좋음
    }

    /**
     * 플레이어의 현재 팀의 스트리머를 가리키도록 나침반 업데이트
     */
    public static void updateCompass(Player player, String streamerName) {
        ItemStack item = player.getInventory().getItem(4);
        if (item != null && item.getType() == Material.COMPASS) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + streamerName + " 팀장");
                item.setItemMeta(meta);
            }
        }
    }
}
