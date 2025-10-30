package com.advancedrace.plugin.gui;

import com.advancedrace.plugin.manager.TeamManager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class TeamSelectInventoryHolder implements InventoryHolder {

    private TeamManager teamManager;

    public TeamSelectInventoryHolder(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
