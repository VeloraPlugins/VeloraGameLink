package online.veloraplugins.gamelink.api.game

enum class GameJoinResult {
    SUCCESS,
    GAME_NOT_FOUND,
    GAME_FULL,
    STATE_NOT_JOINABLE,
    NO_AVAILABLE_GAME,
    REDIS_UNAVAILABLE,
    JOIN_ALREADY_PENDING
}