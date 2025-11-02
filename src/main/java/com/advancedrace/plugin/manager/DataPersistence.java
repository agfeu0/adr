package com.advancedrace.plugin.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DataPersistence {

    private static final String DATA_FOLDER = "plugins/AdvancedRace";
    private static final String SAVE_FILE = "game_data.json";
    private static final Gson gson = new GsonBuilder().create();

    // 메모리에 저장된 팀 정보 (플레이어가 로드될 때까지 유지)
    private static Map<String, String> playerTeamMap = new HashMap<>();
    // 메모리에 저장된 소환된 시청자 정보
    private static Map<String, Set<String>> summonedViewersMap = new HashMap<>();

    /**
     * 게임 상태를 JSON 파일로 저장 (점수 정보, 남은시간, 소환된 시청자 정보 포함)
     */
    public static void saveGameData(TeamManager teamManager, Map<String, Integer> teamScores, long remainingSeconds) {
        try {
            File folder = new File(DATA_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File saveFile = new File(DATA_FOLDER, SAVE_FILE);

            JsonObject root = new JsonObject();

            // 팀 정보 저장
            JsonArray teamsArray = new JsonArray();
            for (String streamerName : teamManager.getStreamerNames()) {
                TeamManager.Team team = teamManager.getTeamByStreamer(streamerName);
                if (team != null) {
                    JsonObject teamObj = new JsonObject();
                    teamObj.addProperty("streamer", streamerName);
                    teamObj.addProperty("color", team.getColor());

                    // 팀원 정보 저장 (플레이어 상태 포함)
                    JsonArray playersArray = new JsonArray();
                    for (Player player : team.getPlayers()) {
                        JsonObject playerObj = new JsonObject();
                        playerObj.addProperty("name", player.getName());
                        playerObj.addProperty("spawnTier", teamManager.getSpawnTier(player)); // SpawnTier 저장
                        playerObj.addProperty("deathCount", teamManager.getDeathCount(player)); // 사망 횟수 저장
                        playersArray.add(playerObj);
                    }
                    teamObj.add("players", playersArray);

                    // 팀 점수 저장
                    int score = teamScores.getOrDefault(streamerName, 0);
                    teamObj.addProperty("score", score);

                    // 소환된 시청자 저장
                    JsonArray summonedArray = new JsonArray();
                    Set<String> summonedViewers = teamManager.getSummonedViewersSet(streamerName);
                    for (String viewerName : summonedViewers) {
                        summonedArray.add(viewerName);
                    }
                    teamObj.add("summoned", summonedArray);

                    teamsArray.add(teamObj);
                }
            }
            root.add("teams", teamsArray);

            // 남은 시간 저장
            root.addProperty("remainingSeconds", remainingSeconds);

            // JSON 파일에 저장
            try (FileWriter writer = new FileWriter(saveFile)) {
                gson.toJson(root, writer);
            }

            Bukkit.getLogger().info("[AdvancedRace] 게임 데이터 저장 완료: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AdvancedRace] 게임 데이터 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * JSON 파일에서 게임 상태를 로드
     */
    public static void loadGameData(TeamManager teamManager) {
        try {
            File saveFile = new File(DATA_FOLDER, SAVE_FILE);

            if (!saveFile.exists()) {
                Bukkit.getLogger().info("[AdvancedRace] 저장된 게임 데이터가 없습니다.");
                return;
            }

            try (FileReader reader = new FileReader(saveFile)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);

                if (root == null || !root.has("teams")) {
                    return;
                }

                JsonArray teamsArray = root.getAsJsonArray("teams");

                // 팀 정보 로드
                for (int i = 0; i < teamsArray.size(); i++) {
                    JsonObject teamObj = teamsArray.get(i).getAsJsonObject();
                    String streamerName = teamObj.get("streamer").getAsString();
                    String color = teamObj.get("color").getAsString();

                    // 팀 생성
                    teamManager.createTeam(streamerName, color);

                    // 플레이어 추가 (상태 정보 포함)
                    JsonArray playersArray = teamObj.getAsJsonArray("players");
                    for (int j = 0; j < playersArray.size(); j++) {
                        JsonElement playerElement = playersArray.get(j);
                        String playerName;
                        int spawnTier = 1; // 기본값
                        int deathCount = 0; // 기본값

                        // 호환성: 문자열 형식(기존)과 객체 형식(새로운) 모두 지원
                        if (playerElement.isJsonObject()) {
                            JsonObject playerObj = playerElement.getAsJsonObject();
                            playerName = playerObj.get("name").getAsString();
                            spawnTier = playerObj.has("spawnTier") ? playerObj.get("spawnTier").getAsInt() : 1;
                            deathCount = playerObj.has("deathCount") ? playerObj.get("deathCount").getAsInt() : 0;
                        } else {
                            playerName = playerElement.getAsString();
                        }

                        Player player = Bukkit.getPlayer(playerName);
                        if (player != null) {
                            teamManager.addPlayerToTeam(player, streamerName);
                            teamManager.setSpawnTier(player, spawnTier);
                            // 사망 횟수 복원 (deathCount만큼 증가)
                            for (int k = 0; k < deathCount; k++) {
                                teamManager.incrementDeathCount(player);
                            }
                        } else {
                            // 플레이어가 오프라인이면 메모리에 저장 (나중에 입장할 때 자동 추가)
                            playerTeamMap.put(playerName, streamerName);
                        }
                    }

                    // 소환된 시청자 정보 복원
                    if (teamObj.has("summoned")) {
                        JsonArray summonedArray = teamObj.getAsJsonArray("summoned");
                        Set<String> summonedViewers = new HashSet<>();
                        for (int j = 0; j < summonedArray.size(); j++) {
                            summonedViewers.add(summonedArray.get(j).getAsString());
                        }
                        teamManager.setSummonedViewers(streamerName, summonedViewers);
                    }
                }

                Bukkit.getLogger().info("[AdvancedRace] 게임 데이터 로드 완료");
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AdvancedRace] 게임 데이터 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 저장된 팀별 점수 로드
     */
    public static Map<String, Integer> loadTeamScores() {
        Map<String, Integer> scores = new HashMap<>();
        try {
            File saveFile = new File(DATA_FOLDER, SAVE_FILE);

            if (!saveFile.exists()) {
                return scores;
            }

            try (FileReader reader = new FileReader(saveFile)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);

                if (root == null || !root.has("teams")) {
                    return scores;
                }

                JsonArray teamsArray = root.getAsJsonArray("teams");

                // 팀별 점수 로드
                for (int i = 0; i < teamsArray.size(); i++) {
                    JsonObject teamObj = teamsArray.get(i).getAsJsonObject();
                    String streamerName = teamObj.get("streamer").getAsString();
                    int score = teamObj.has("score") ? teamObj.get("score").getAsInt() : 0;
                    scores.put(streamerName, score);
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AdvancedRace] 팀 점수 로드 실패: " + e.getMessage());
        }
        return scores;
    }

    /**
     * 저장된 남은 시간 로드 (초 단위)
     */
    public static long loadRemainingSeconds() {
        try {
            File saveFile = new File(DATA_FOLDER, SAVE_FILE);

            if (!saveFile.exists()) {
                return 0;
            }

            try (FileReader reader = new FileReader(saveFile)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);

                if (root == null || !root.has("remainingSeconds")) {
                    return 0;
                }

                return root.get("remainingSeconds").getAsLong();
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AdvancedRace] 남은 시간 로드 실패: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 저장된 게임 데이터 삭제
     */
    public static void deleteGameData() {
        try {
            File saveFile = new File(DATA_FOLDER, SAVE_FILE);
            if (saveFile.exists()) {
                saveFile.delete();
                Bukkit.getLogger().info("[AdvancedRace] 게임 데이터 삭제 완료");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AdvancedRace] 게임 데이터 삭제 실패: " + e.getMessage());
        }
    }

    /**
     * 저장된 팀 정보에서 플레이어의 스트리머 이름 조회
     */
    public static String getStreamerForPlayer(String playerName) {
        // playerTeamMap에서 먼저 확인
        String streamerName = playerTeamMap.get(playerName);
        if (streamerName != null) {
            return streamerName; // 제거하지 말고 유지
        }

        // playerTeamMap에 없으면 game_data.json에서 직접 조회
        try {
            File saveFile = new File(DATA_FOLDER, SAVE_FILE);
            if (!saveFile.exists()) {
                return null;
            }

            try (FileReader reader = new FileReader(saveFile)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("teams")) {
                    return null;
                }

                JsonArray teamsArray = root.getAsJsonArray("teams");
                for (int i = 0; i < teamsArray.size(); i++) {
                    JsonObject teamObj = teamsArray.get(i).getAsJsonObject();
                    JsonArray playersArray = teamObj.getAsJsonArray("players");

                    for (int j = 0; j < playersArray.size(); j++) {
                        JsonElement playerElement = playersArray.get(j);
                        String currentPlayerName = null;

                        // 호환성: 문자열 형식(기존)과 객체 형식(새로운) 모두 지원
                        if (playerElement.isJsonObject()) {
                            JsonObject playerObj = playerElement.getAsJsonObject();
                            currentPlayerName = playerObj.get("name").getAsString();
                        } else if (playerElement.isJsonPrimitive()) {
                            currentPlayerName = playerElement.getAsString();
                        }

                        if (currentPlayerName != null && currentPlayerName.equals(playerName)) {
                            return teamObj.get("streamer").getAsString();
                        }
                    }
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AdvancedRace] 플레이어 팀 정보 조회 실패: " + e.getMessage());
        }

        return null;
    }
}
