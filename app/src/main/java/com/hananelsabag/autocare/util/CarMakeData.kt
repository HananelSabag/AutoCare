package com.hananelsabag.autocare.util

/**
 * Curated list of car makes popular in Israel, with common models per make.
 * Used for ExposedDropdownMenuBox suggestions in AddCarSheet.
 * Free-text is always allowed as fallback.
 */
val CAR_MAKES: List<String> = listOf(
    "Toyota", "Hyundai", "Kia", "Mazda", "Honda",
    "Ford", "Volkswagen", "BMW", "Mercedes-Benz", "Audi",
    "Nissan", "Subaru", "Mitsubishi", "Renault", "Peugeot",
    "Fiat", "Volvo", "Skoda", "SEAT", "Opel",
    "Chevrolet", "Jeep", "Land Rover", "Lexus", "Suzuki",
    "Dacia", "MG", "BYD", "Chery", "Geely",
    "Alfa Romeo", "Citroën", "MINI", "Infiniti", "Acura"
)

val CAR_MODELS: Map<String, List<String>> = mapOf(
    "Toyota" to listOf(
        "Corolla", "Camry", "RAV4", "Yaris", "C-HR",
        "Prius", "Land Cruiser", "Hilux", "Auris", "Aygo",
        "Highlander", "bZ4X", "Supra"
    ),
    "Hyundai" to listOf(
        "i10", "i20", "i25", "i30", "i35",
        "Tucson", "Santa Fe", "Ioniq 5", "Ioniq 6", "Kona",
        "Elantra", "Sonata", "Staria"
    ),
    "Kia" to listOf(
        "Picanto", "Rio", "Ceed", "Sportage", "Stonic",
        "Sorento", "Niro", "EV6", "Carnival", "Telluride"
    ),
    "Mazda" to listOf(
        "Mazda2", "Mazda3", "Mazda6", "CX-3", "CX-30",
        "CX-5", "CX-60", "MX-5"
    ),
    "Honda" to listOf(
        "Jazz", "Civic", "Accord", "CR-V", "HR-V",
        "ZR-V", "e:Ny1"
    ),
    "Ford" to listOf(
        "Fiesta", "Focus", "Mondeo", "Kuga", "Puma",
        "Mustang", "Mustang Mach-E", "Ranger", "Explorer", "EcoSport"
    ),
    "Volkswagen" to listOf(
        "Polo", "Golf", "Passat", "Tiguan", "T-Roc",
        "T-Cross", "ID.3", "ID.4", "Touareg", "Arteon"
    ),
    "BMW" to listOf(
        "Series 1", "Series 2", "Series 3", "Series 5", "Series 7",
        "X1", "X2", "X3", "X5", "X7",
        "iX", "i4", "i7"
    ),
    "Mercedes-Benz" to listOf(
        "A-Class", "B-Class", "C-Class", "E-Class", "S-Class",
        "CLA", "GLA", "GLB", "GLC", "GLE",
        "EQA", "EQB", "EQC"
    ),
    "Audi" to listOf(
        "A1", "A3", "A4", "A5", "A6", "A7",
        "Q2", "Q3", "Q5", "Q7", "Q8",
        "e-tron", "e-tron GT"
    ),
    "Nissan" to listOf(
        "Micra", "Note", "Juke", "Qashqai", "X-Trail",
        "Leaf", "Ariya", "Navara"
    ),
    "Subaru" to listOf(
        "Impreza", "Legacy", "Forester", "Outback",
        "XV", "Crosstrek", "WRX", "BRZ"
    ),
    "Mitsubishi" to listOf(
        "Colt", "Lancer", "ASX", "Eclipse Cross",
        "Outlander", "Outlander PHEV", "L200"
    ),
    "Renault" to listOf(
        "Twingo", "Clio", "Megane", "Kadjar", "Koleos",
        "Fluence", "Duster", "Captur", "Zoe", "Arkana"
    ),
    "Peugeot" to listOf(
        "108", "208", "308", "508",
        "2008", "3008", "5008", "e-208", "e-2008"
    ),
    "Fiat" to listOf(
        "500", "500X", "500e", "Punto", "Tipo", "Doblo"
    ),
    "Volvo" to listOf(
        "S60", "S90", "V60", "V90",
        "XC40", "XC60", "XC90",
        "C40 Recharge", "EX30", "EX90"
    ),
    "Skoda" to listOf(
        "Fabia", "Scala", "Octavia", "Superb",
        "Kamiq", "Karoq", "Kodiaq", "Enyaq"
    ),
    "SEAT" to listOf(
        "Ibiza", "Leon", "Arona", "Ateca", "Tarraco"
    ),
    "Opel" to listOf(
        "Corsa", "Astra", "Insignia", "Mokka",
        "Crossland", "Grandland", "Corsa-e", "Mokka-e"
    ),
    "Chevrolet" to listOf(
        "Spark", "Aveo", "Cruze", "Trax",
        "Equinox", "Captiva", "Trailblazer"
    ),
    "Jeep" to listOf(
        "Renegade", "Compass", "Cherokee", "Grand Cherokee",
        "Wrangler", "Gladiator", "Avenger"
    ),
    "Land Rover" to listOf(
        "Freelander", "Discovery Sport", "Discovery",
        "Range Rover Evoque", "Range Rover Sport", "Range Rover",
        "Defender"
    ),
    "Lexus" to listOf(
        "IS", "ES", "LS", "UX", "NX", "RX", "LX", "RC"
    ),
    "Suzuki" to listOf(
        "Alto", "Swift", "Ignis", "Baleno", "Vitara",
        "S-Cross", "SX4", "Jimny"
    ),
    "Dacia" to listOf(
        "Sandero", "Logan", "Duster", "Jogger", "Spring"
    ),
    "MG" to listOf(
        "3", "5", "ZS", "ZS EV", "HS", "4", "Marvel R"
    ),
    "BYD" to listOf(
        "Atto 3", "Seal", "Seal U", "Dolphin", "Han", "Tang"
    ),
    "Chery" to listOf(
        "Tiggo 4", "Tiggo 5x", "Tiggo 7", "Tiggo 8"
    ),
    "Geely" to listOf(
        "Coolray", "Emgrand", "Tugella", "Okavango"
    ),
    "Alfa Romeo" to listOf(
        "Giulietta", "Giulia", "Stelvio", "Tonale"
    ),
    "Citroën" to listOf(
        "C1", "C3", "C4", "C5 Aircross", "ë-C4"
    ),
    "MINI" to listOf(
        "Cooper", "Cooper S", "Clubman", "Countryman",
        "Paceman", "Electric"
    ),
    "Infiniti" to listOf(
        "Q30", "Q50", "Q60", "QX30", "QX50", "QX60"
    ),
    "Acura" to listOf(
        "ILX", "TLX", "MDX", "RDX"
    )
)

/** Returns model suggestions for a given make, or empty list if make is unknown. */
fun modelsForMake(make: String): List<String> =
    CAR_MODELS[make] ?: emptyList()
