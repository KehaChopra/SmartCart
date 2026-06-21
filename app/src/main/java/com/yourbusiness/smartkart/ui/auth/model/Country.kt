package com.yourbusiness.smartkart.ui.auth.model

data class Country(
    val name: String,
    val isoCode: String,
    val dialCode: String
) {
    val flagEmoji: String
        get() = isoCode
            .uppercase()
            .map { char ->
                Character.toChars(0x1F1E6 + (char.code - 'A'.code)).concatToString()
            }
            .joinToString("")

    companion object {


        val countries = listOf(
            Country("United States", "US", "+1"),
            Country("India", "IN", "+91"),
            Country("United Kingdom", "GB", "+44"),
            Country("Canada", "CA", "+1"),
            Country("Australia", "AU", "+61"),
            Country("Germany", "DE", "+49"),
            Country("France", "FR", "+33"),
            Country("Japan", "JP", "+81"),
            Country("South Korea", "KR", "+82"),
            Country("Brazil", "BR", "+55"),
            Country("Mexico", "MX", "+52"),
            Country("Singapore", "SG", "+65"),
            Country("United Arab Emirates", "AE", "+971"),
            Country("South Africa", "ZA", "+27"),
            Country("Nigeria", "NG", "+234"),
            Country("Philippines", "PH", "+63"),
            Country("Indonesia", "ID", "+62"),
            Country("Pakistan", "PK", "+92"),
            Country("Bangladesh", "BD", "+880"),
            Country("China", "CN", "+86")
        ).sortedBy { it.name }
        val default = countries.first { it.isoCode == "IN" }
    }
}
