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
}
