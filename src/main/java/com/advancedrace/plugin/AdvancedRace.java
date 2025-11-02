package com.advancedrace.plugin;

import com.advancedrace.plugin.command.GameEndCommand;
import com.advancedrace.plugin.command.GameStartCommand;
import com.advancedrace.plugin.command.StreamerCommand;
import com.advancedrace.plugin.command.TeamSelectCommand;
import com.advancedrace.plugin.listener.AdvancementListener;
import com.advancedrace.plugin.listener.BeaconListener;
import com.advancedrace.plugin.listener.CompassInteractListener;
import com.advancedrace.plugin.listener.GUIListener;
import com.advancedrace.plugin.listener.HardcoreDeathListener;
import com.advancedrace.plugin.listener.InventorySizeListener;
import com.advancedrace.plugin.listener.PlayerChatListener;
import com.advancedrace.plugin.listener.PlayerDeathListener;
import com.advancedrace.plugin.listener.PlayerNameListener;
import com.advancedrace.plugin.listener.PvPListener;
import com.advancedrace.plugin.listener.StreamerDeathListener;
import com.advancedrace.plugin.manager.AdvancementManager;
import com.advancedrace.plugin.manager.DataPersistence;
import com.advancedrace.plugin.manager.GameStateManager;
import com.advancedrace.plugin.manager.TeamManager;
import com.advancedrace.plugin.manager.ViewerSummonManager;
import com.advancedrace.plugin.task.DistanceLimitTask;
import com.advancedrace.plugin.task.GameTimerTask;
import com.advancedrace.plugin.util.ViewerInitializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class AdvancedRace extends JavaPlugin {

    private static AdvancedRace instance;
    private TeamManager teamManager;
    private AdvancementManager advancementManager;
    private ViewerSummonManager viewerSummonManager;
    private GameStateManager gameStateManager;
    private GameTimerTask gameTimerTask;
    private AdvancementListener advancementListener;

    @Override
    public void onEnable() {
        instance = this;

        // Config 파일 생성 및 로드
        saveDefaultConfig();
        reloadConfig();

        // 팀 매니저 초기화
        teamManager = new TeamManager();

        // 발전과제 매니저 초기화
        advancementManager = new AdvancementManager();

        // 시청자 소환 매니저 초기화
        viewerSummonManager = new ViewerSummonManager(teamManager);

        // 게임 상태 매니저 초기화
        gameStateManager = new GameStateManager();

        // 저장된 게임 데이터 로드 시도
        DataPersistence.loadGameData(teamManager);

        // 로드 후 플레이어 디스플레이 및 나침반 업데이트 (1틱 지연)
        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                PlayerNameListener.updatePlayerDisplay(player, teamManager);
                // 플레이어가 팀에 속하면 나침반 업데이트
                TeamManager.Team team = teamManager.getTeam(player);
                if (team != null) {
                    ViewerInitializer.updateCompass(player, team.getStreamer());
                }
            }
        }, 1);

        // 명령어 등록
        StreamerCommand streamerCommand = new StreamerCommand(teamManager);
        if (getCommand("스트리머") != null) {
            getCommand("스트리머").setExecutor(streamerCommand);
            getCommand("스트리머").setTabCompleter(streamerCommand);
        }
        if (getCommand("팀선택") != null) {
            getCommand("팀선택").setExecutor(new TeamSelectCommand(teamManager));
        }
        if (getCommand("시작") != null) {
            getCommand("시작").setExecutor(new GameStartCommand(gameStateManager, teamManager));
            getLogger().info("[AdvancedRace] /시작 명령어 등록됨");
        } else {
            getLogger().warning("[AdvancedRace] /시작 명령어를 찾을 수 없습니다!");
        }
        if (getCommand("종료") != null) {
            getCommand("종료").setExecutor(new GameEndCommand(gameStateManager, teamManager));
            getLogger().info("[AdvancedRace] /종료 명령어 등록됨");
        } else {
            getLogger().warning("[AdvancedRace] /종료 명령어를 찾을 수 없습니다!");
        }

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new BeaconListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new InventorySizeListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new HardcoreDeathListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new PvPListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new StreamerDeathListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new CompassInteractListener(teamManager), this);
        advancementListener = new AdvancementListener(teamManager, advancementManager, viewerSummonManager);
        getServer().getPluginManager().registerEvents(advancementListener, this);
        getServer().getPluginManager().registerEvents(new PlayerNameListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(teamManager), this);

        // 저장된 팀 점수 로드
        Map<String, Integer> savedScores = DataPersistence.loadTeamScores();
        for (String streamerName : savedScores.keySet()) {
            advancementListener.setTeamScore(streamerName, savedScores.get(streamerName));
        }

        // Task 등록 (거리 제한 체크: 3초마다)
        new DistanceLimitTask(teamManager).runTaskTimer(this, 0, 60);

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

    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    public void setGameTimerTask(GameTimerTask timerTask) {
        this.gameTimerTask = timerTask;
    }

    public GameTimerTask getGameTimerTask() {
        return gameTimerTask;
    }

    /**
     * Config에서 게임 시간 가져오기 (초 단위)
     */
    public long getGameDurationSeconds() {
        return getConfig().getLong("game-duration-seconds", 3600);
    }

    /**
     * Config에서 랜덤 텔레포트 범위 가져오기 (블록 단위)
     */
    public int getRandomTeleportRange() {
        return getConfig().getInt("random-teleport-range", 3000);
    }

    public AdvancementListener getAdvancementListener() {
        return advancementListener;
    }

    public AdvancementManager getAdvancementManager() {
        return advancementManager;
    }
}
