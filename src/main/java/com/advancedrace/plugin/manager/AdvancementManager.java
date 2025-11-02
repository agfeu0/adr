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
     * 일반(노란색): 1명 소환
     * 특수(보라색): 3명 소환
     */
    public int getAdvancementType(String advancementName) {
        // 특수 발전과제 (보라색) 판정
        if (advancementName.contains("end/kill_dragon") ||    // 엔드 - 드래곤 처치
            advancementName.contains("nether/summon_wither") || // 네더 - 위더 소환
            advancementName.contains("adventure/kill_all_mobs") || // 모험 - 모든 몹 처치
            advancementName.contains("adventure/hero_of_the_village") || // 모험 - 마을 영웅
            advancementName.contains("adventure/very_very_frightening")) { // 모험 - 번개 처치
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
