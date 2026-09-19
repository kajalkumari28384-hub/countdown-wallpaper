package com.countdown.wallpaper

enum class CardType { TEXT, CHALLENGE, EMOJI, BOOK, FORMULA, IMAGE }

data class MotivationCard(
    val type: CardType,
    val title: String,
    val subtitle: String? = null,
    val accentColor: Int? = null,
    val imagePath: String? = null
)
