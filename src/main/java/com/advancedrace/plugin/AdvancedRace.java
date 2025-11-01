package com.advancedrace.plugin;

import com.advancedrace.plugin.command.StreamerCommand;
import com.advancedrace.plugin.command.TeamSelectCommand;
import com.advancedrace.plugin.command.TeleportCommand;
import com.advancedrace.plugin.listener.AdvancementListener;
import com.advancedrace.plugin.listener.BeaconListener;
import com.advancedrace.plugin.listener.GUIListener;
import com.advancedrace.plugin.listener.HardcoreDeathListener;
import com.advancedrace.plugin.listener.InventorySizeListener;
import com.advancedrace.plugin.listener.PlayerChatListener;
import com.advancedrace.plugin.listener.PlayerDeathListener;
import com.advancedrace.plugin.listener.PlayerNameListener;
import com.advancedrace.plugin.listener.PvPListener;
import com.advancedrace.plugin.listener.StreamerDeathListener;
import com.advancedrace.plugin.manager.AdvancementManager;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.manager.ViewerSummonManager;
import com.advancedrace.plugin.task.DistanceLimitTask;
import org.bukkit.plugin.java.JavaPlugin;

public class AdvancedRace extends JavaPlugin {

    private static AdvancedRace instance;
    private TeamManager teamManager;
    private AdvancementManager advancementManager;
    private ViewerSummonManager viewerSummonManager;

    @Override
    public void onEnable() {
        instance = this;

        // 팀 매니저 초기화
        teamManager = new TeamManager();

        // 발전과제 매니저 초기화
        advancementManager = new AdvancementManager();

        // 시청자 소환 매니저 초기화
        viewerSummonManager = new ViewerSummonManager(teamManager);

        // 명령어 등록
        StreamerCommand streamerCommand = new StreamerCommand(teamManager);
        getCommand("스트리머").setExecutor(streamerCommand);
        getCommand("스트리머").setTabCompleter(streamerCommand);
        getCommand("팀선택").setExecutor(new TeamSelectCommand(teamManager));
        getCommand("발전과제_tp").setExecutor(new TeleportCommand());

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new BeaconListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new InventorySizeListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new HardcoreDeathListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new PvPListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new StreamerDeathListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new AdvancementListener(teamManager, advancementManager, viewerSummonManager), this);
        getServer().getPluginManager().registerEvents(new PlayerNameListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(teamManager), this);

        // Task 등록 (거리 제한 체크: 1초마다)
        new DistanceLimitTask(teamManager).runTaskTimer(this, 0, 20);

        getLogger().info("AdvancedRace 플러그인 활성화됨!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AdvancedRace 플러그인 비활성화됨!");
    }

    public static AdvancedRace getInstance() {
        return instance;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
