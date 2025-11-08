package com.advancedrace.plugin.util;

import com.advancedrace.plugin.AdvancedRace;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Random;

public class SafeTeleporter {

    private static final Random random = new Random();
    private static final int MAX_ATTEMPTS = 100; // 최대 시도 횟수
    private static final Material[] PASSABLE_MATERIALS = {
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR
    };

    /**
     * 월드보더 범위 내에서 안전한 위치를 찾아 플레이어를 텔레포트
     *
     * @param player 텔레포트할 플레이어
     * @return 텔레포트 성공 여부
     */
    public static boolean teleportToSafeLocation(Player player) {
        World world = player.getWorld();
        WorldBorder border = world.getWorldBorder();

        // 월드보더의 중심
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();

        // Config에서 설정한 랜덤 텔레포트 범위 가져오기
        int configRange = AdvancedRace.getInstance().getRandomTeleportRange();

        // 월드보더 크기와 Config 범위 중 작은 값 사용
        double borderSize = border.getSize();
        double range = Math.min(configRange * 2.0, borderSize);

        // 텔레포트 범위 계산 (경계에서 50블록 떨어진 안전 지역)
        double minX = centerX - (range / 2) + 50;
        double maxX = centerX + (range / 2) - 50;
        double minZ = centerZ - (range / 2) + 50;
        double maxZ = centerZ + (range / 2) - 50;

        // 안전한 위치 찾기
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // 랜덤 좌표 생성
            double randomX = minX + (random.nextDouble() * (maxX - minX));
            double randomZ = minZ + (random.nextDouble() * (maxZ - minZ));

            // 안전한 높이 찾기 (y=150부터 y=80까지 탐색 - 노출된 지표면)
            for (int y = 150; y >= 80; y--) {
                Block footBlock = world.getBlockAt((int) randomX, y - 1, (int) randomZ);
                Block bodyBlock = world.getBlockAt((int) randomX, y, (int) randomZ);
                Block headBlock = world.getBlockAt((int) randomX, y + 1, (int) randomZ);

                // 안전한 블록인지 확인
                if (isSafeBlock(footBlock) && isSafeBlock(bodyBlock) && isSafeBlock(headBlock)) {
                    // 발 아래는 실제 고체 블록이 있어야 함 (동굴 제외)
                    Block groundBlock = world.getBlockAt((int) randomX, y - 2, (int) randomZ);
                    if (!groundBlock.isPassable() && isGroundBlock(groundBlock)) {
                        // Y좌표 위에서 실제 고체 블록이 있는지 확인 (동굴 내부 제외)
                        boolean hasSolidBlockAbove = false;
                        for (int checkY = y + 2; checkY <= y + 20; checkY++) {
                            Block checkBlock = world.getBlockAt((int) randomX, checkY, (int) randomZ);
                            if (!checkBlock.isPassable() && !checkBlock.getType().equals(Material.WATER) && !checkBlock.getType().equals(Material.LAVA)) {
                                hasSolidBlockAbove = true;
                                break;
                            }
                        }

                        if (hasSolidBlockAbove) {
                            Location safeLocation = new Location(world, randomX + 0.5, y, randomZ + 0.5);
                            player.teleport(safeLocation);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 블록이 안전한지 확인 (플레이어가 통과할 수 있는지 - 블록끼지 않도록)
     */
    private static boolean isSafeBlock(Block block) {
        Material type = block.getType();

        // 통과 가능한 블록만 허용
        for (Material passable : PASSABLE_MATERIALS) {
            if (type == passable) {
                return true;
            }
        }

        // 그 외 블록은 안전하지 않음 (물, 용암, 일반 블록 등)
        return false;
    }

    /**
     * 블록이 지면으로 사용될 수 있는지 확인 (잔디, 눈, 얼음, 나뭇잎)
     */
    private static boolean isGroundBlock(Block block) {
        Material type = block.getType();

        // 잔디, 눈, 얼음, 나뭇잎 허용
        return type == Material.GRASS_BLOCK ||
               type == Material.SNOW_BLOCK ||
               type == Material.SNOW ||
               type == Material.ICE ||
               type == Material.PACKED_ICE ||
               type == Material.OAK_LEAVES ||
               type == Material.SPRUCE_LEAVES ||
               type == Material.BIRCH_LEAVES ||
               type == Material.JUNGLE_LEAVES ||
               type == Material.ACACIA_LEAVES ||
               type == Material.DARK_OAK_LEAVES ||
               type == Material.MANGROVE_LEAVES ||
               type == Material.CHERRY_LEAVES ||
               type == Material.AZALEA_LEAVES;
    }

}
