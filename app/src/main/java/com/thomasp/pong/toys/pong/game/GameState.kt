package com.thomasjprice.pong.toys.pong.game

enum class GameState {
    START_SCREEN,    // Showing level information
    PLAYING,         // Active game
    LEVEL_UP,       // Player won
    GAME_OVER,      // Player lost
}
