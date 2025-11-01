package com.advancedrace.plugin.manager;

/**
 * 게임 상태 관리
 */
public class GameStateManager {

    public enum GameState {
        WAITING,  // 대기 중 (게임 시작 전)
        RUNNING   // 게임 진행 중
    }

    private GameState currentState = GameState.WAITING;

    public GameStateManager() {
    }

    /**
     * 게임 시작
     */
    public void startGame() {
        currentState = GameState.RUNNING;
    }

    /**
     * 게임 종료
     */
    public void endGame() {
        currentState = GameState.WAITING;
    }

    /**
     * 현재 게임 상태 반환
     */
    public GameState getState() {
        return currentState;
    }

    /**
     * 게임이 실행 중인지 확인
     */
    public boolean isRunning() {
        return currentState == GameState.RUNNING;
    }

    /**
     * 게임이 대기 중인지 확인
     */
    public boolean isWaiting() {
        return currentState == GameState.WAITING;
    }
}
