package com.advancedrace.plugin.manager;

import java.util.HashSet;
import java.util.Set;

public class AdvancementManager {

    private Set<String> completedAdvancements = new HashSet<>();

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
     * 발전과제 유형 구분 (일반 vs 특수)
     * 예: minecraft:story/* = 일반, minecraft:end/* = 특수
     */
    public int getAdvancementType(String advancementName) {
        // 특수 발전과제 판정 (엔드, 네더, 관련된 것들)
        if (advancementName.contains("end/") ||
            advancementName.contains("nether/") ||
            advancementName.contains("husbandry/breed_an_animal") ||
            advancementName.contains("adventure/")) {
            return 3; // 특수 발전과제 = 3명 소환
        }
        return 1; // 일반 발전과제 = 1명 소환
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
}
