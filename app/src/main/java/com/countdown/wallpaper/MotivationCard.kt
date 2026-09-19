package com.countdown.wallpaper

enum class CardType { TEXT, CHALLENGE, EMOJI, FORMULA, IMAGE }

data class MotivationCard(
    val type: CardType,
    val title: String,
    val imagePath: String? = null
)
