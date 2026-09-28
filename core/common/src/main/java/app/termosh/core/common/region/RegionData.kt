package app.termosh.core.common.region

/**
 * Статические наборы для регионального скоринга.
 * Пока только Россия (бета). Остальные регионы СНГ — на следующем этапе.
 */
object RegionData {

    /** Языки локали. Сравнивается только language, без country. */
    val RUSSIAN_LANGUAGES: Set<String> = setOf("ru")

    /** Часовые пояса России (включая алиасы для старых tzdata). */
    val RUSSIAN_TIMEZONES: Set<String> = setOf(
        "Europe/Moscow",
        "Europe/Kaliningrad",
        "Europe/Samara",
        "Europe/Volgograd",
        "Europe/Saratov",
        "Europe/Astrakhan",
        "Europe/Ulyanovsk",
        "Europe/Kirov",
        "Asia/Yekaterinburg",
        "Asia/Omsk",
        "Asia/Novosibirsk",
        "Asia/Barnaul",
        "Asia/Tomsk",
        "Asia/Novokuznetsk",
        "Asia/Krasnoyarsk",
        "Asia/Irkutsk",
        "Asia/Chita",
        "Asia/Yakutsk",
        "Asia/Khandyga",
        "Asia/Vladivostok",
        "Asia/Ust-Nera",
        "Asia/Magadan",
        "Asia/Sakhalin",
        "Asia/Srednekolymsk",
        "Asia/Kamchatka",
        "Asia/Anadyr",
    )

    /** MCC мобильных сетей России. */
    val RUSSIAN_MCCS: Set<String> = setOf("250")

    /**
     * Маркеры региона в Build.FINGERPRINT.
     * Голое "ru" не ищем — ложно срабатывает на true/product/structure.
     */
    val FINGERPRINT_MARKERS: List<String> = listOf(
        "/ru/", "_ru_", "-ru-", "ru_ru", "russia", "/cis/", "_cis_", "cis_",
    )
}
