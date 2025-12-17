package io.strategiz.data.strategy.entity;

/**
 * Type of strategy deployment.
 * Determines what happens when a strategy produces signals.
 */
public enum DeploymentType {

    /**
     * Alert deployment - sends notifications when signals are detected.
     * Channels: email, SMS, push, in-app.
     */
    ALERT,

    /**
     * Bot deployment - executes trades automatically when signals are detected.
     * Requires provider integration (Alpaca, Coinbase, etc.).
     */
    BOT,

    /**
     * Paper trading - simulates trades without real execution.
     * For testing and validation.
     */
    PAPER
}
