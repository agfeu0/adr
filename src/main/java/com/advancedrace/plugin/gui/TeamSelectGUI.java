package com.advancedrace.plugin.gui;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TeamSelectGUI {

    private TeamManager teamManager;

    public TeamSelectGUI(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void openGUI(Player player) {
        List<String> streamerNames = teamManager.getStreamerNames();

        if (streamerNames.isEmpty()) {
            player.sendMessage("등록된 스트리머가 없습니다.");
            return;
        }

        // GUI 크기 계산 (최소 9칸, 9칸 단위)
        int rows = (int) Math.ceil(streamerNames.size() / 9.0);
        int size = Math.min(rows * 9, 54);
        size = Math.max(size, 9); // 최소 9칸

        Inventory inventory = Bukkit.createInventory(
                new TeamSelectInventoryHolder(teamManager),
                size,
                "팀 선택"
        );

        // 스트리머별 팀 정보 추가
        int slot = 0;
        for (String streamerName : streamerNames) {
            TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
            if (team == null) continue;

            ItemStack item = createTeamItem(streamerName, team);
            inventory.setItem(slot, item);
            slot++;
        }

        player.openInventory(inventory);
    }

    private ItemStack createTeamItem(String streamerName, TeamManager.Team team) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§b" + streamerName);
            List<String> lore = new ArrayList<>();
            lore.add("§7팀원: " + team.getPlayerCount() + "명");
            lore.add("");
            lore.add("§a클릭하여 팀 선택");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}
