package com.advancedrace.plugin.manager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AdvancementManager {

    private Set<String> completedAdvancements = new HashSet<>();
    private static final String DATA_FOLDER = "plugins/AdvancedRace";
    private static final String ADVANCEMENT_FILE = "completed_advancements.json";
    private static final Gson gson = new Gson();

    /**
     * 발전과제가 이미 완료되었는지 확인
     */
    public boolean isCompleted(String advancementName) {
        return completedAdvancements.contains(advancementName);
    }

    /**
     * 발전과제를 완료된 것으로 표시
     */
    public void markCompleted(String advancementName) {
        completedAdvancements.add(advancementName);
    }

    /**
     * @deprecated 더 이상 사용되지 않음. AdvancementListener에서 Display 정보로 일반/특수 판정
     */
    @Deprecated
    public int getAdvancementType(String advancementName) {
        return 1; // 기본값
    }

    /**
     * 완료된 발전과제 수
     */
    public int getCompletedCount() {
        return completedAdvancements.size();
    }

    /**
     * 모든 발전과제 초기화 (테스트용)
     */
    public void resetAdvancements() {
        completedAdvancements.clear();
    }

    /**
     * 완료된 발전과제를 파일에 저장
     */
    public void saveCompletedAdvancements() {
        try {
            File folder = new File(DATA_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File saveFile = new File(DATA_FOLDER, ADVANCEMENT_FILE);

            JsonObject root = new JsonObject();
            JsonArray advancementsArray = new JsonArray();

            for (String advancement : completedAdvancements) {
                advancementsArray.add(advancement);
            }

            root.add("completedAdvancements", advancementsArray);

            try (FileWriter writer = new FileWriter(saveFile)) {
                gson.toJson(root, writer);
            }
        } catch (IOException e) {
            System.err.println("[AdvancedRace] 발전과제 데이터 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 파일에서 완료된 발전과제를 로드
     */
    public void loadCompletedAdvancements() {
        try {
            File loadFile = new File(DATA_FOLDER, ADVANCEMENT_FILE);

            if (!loadFile.exists()) {
                return; // 파일이 없으면 그냥 반환
            }

            try (FileReader reader = new FileReader(loadFile)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);

                if (root != null && root.has("completedAdvancements")) {
                    JsonArray advancementsArray = root.getAsJsonArray("completedAdvancements");

                    completedAdvancements.clear();
                    for (JsonElement element : advancementsArray) {
                        completedAdvancements.add(element.getAsString());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[AdvancedRace] 발전과제 데이터 로드 실패: " + e.getMessage());
        }
    }
}
