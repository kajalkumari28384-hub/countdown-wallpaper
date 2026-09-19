package com.countdown.wallpaper

/**
 * ================================================================
 *  अपना कंटेंट यहीं जोड़ें/बदलें — किसी और फाइल को छूने की ज़रूरत नहीं।
 *  (Book covers अब सिर्फ app से गैलरी से अपलोड होंगे, यहाँ नहीं)
 * ================================================================
 */
object MotivationContent {

    fun defaultCards(): List<MotivationCard> = listOf(
        MotivationCard(CardType.CHALLENGE, "Do 15 push-ups"),
        MotivationCard(CardType.CHALLENGE, "Drink a glass of water"),
        MotivationCard(CardType.CHALLENGE, "Stretch for 60 seconds"),
        MotivationCard(CardType.CHALLENGE, "Read 5 pages"),

        MotivationCard(CardType.TEXT, "Discipline beats motivation."),
        MotivationCard(CardType.TEXT, "Small steps, every day."),
        MotivationCard(CardType.TEXT, "Future you is watching."),
        MotivationCard(CardType.TEXT, "Done is better than perfect."),

        MotivationCard(CardType.EMOJI, "🔥 🎯 📈"),
        MotivationCard(CardType.EMOJI, "🚀 keep going"),

        MotivationCard(CardType.CHALLENGE, "Revise today's notes"),
        MotivationCard(CardType.CHALLENGE, "Solve 5 practice problems"),

        MotivationCard(CardType.FORMULA, "E = mc²"),
        MotivationCard(CardType.FORMULA, "a² + b² = c²"),
        MotivationCard(CardType.FORMULA, "F = ma"),
        MotivationCard(CardType.FORMULA, "PV = nRT")
    )
}
