package com.example.domain.converter

import kotlin.math.PI

enum class UnitCategory(val id: String, val displayName: String) {
    LENGTH("length", "Length"),
    WEIGHT("weight", "Weight"),
    TEMPERATURE("temperature", "Temperature"),
    AREA("area", "Area"),
    VOLUME("volume", "Volume"),
    SPEED("speed", "Speed"),
    TIME("time", "Time"),
    DATA_STORAGE("data_storage", "Data Storage (SI)"),
    PRESSURE("pressure", "Pressure"),
    ENERGY("energy", "Energy"),
    POWER("power", "Power"),
    ANGLE("angle", "Angle"),
    FREQUENCY("frequency", "Frequency"),
    FUEL_CONSUMPTION("fuel", "Fuel Consumption"),
    DIGITAL_STORAGE("digital_storage", "Digital Storage (Binary)")
}

data class UnitDefinition(
    val id: String,
    val name: String,
    val symbol: String,
    val category: UnitCategory,
    val toBaseFactor: Double = 1.0 // For linear units: value * toBaseFactor = baseValue
)

object UnitConverterEngine {

    val unitsByCategory: Map<UnitCategory, List<UnitDefinition>> = mapOf(
        UnitCategory.LENGTH to listOf(
            UnitDefinition("m", "Meter", "m", UnitCategory.LENGTH, 1.0),
            UnitDefinition("km", "Kilometer", "km", UnitCategory.LENGTH, 1000.0),
            UnitDefinition("cm", "Centimeter", "cm", UnitCategory.LENGTH, 0.01),
            UnitDefinition("mm", "Millimeter", "mm", UnitCategory.LENGTH, 0.001),
            UnitDefinition("um", "Micrometer", "µm", UnitCategory.LENGTH, 1e-6),
            UnitDefinition("nm", "Nanometer", "nm", UnitCategory.LENGTH, 1e-9),
            UnitDefinition("mi", "Mile", "mi", UnitCategory.LENGTH, 1609.344),
            UnitDefinition("yd", "Yard", "yd", UnitCategory.LENGTH, 0.9144),
            UnitDefinition("ft", "Foot", "ft", UnitCategory.LENGTH, 0.3048),
            UnitDefinition("in", "Inch", "in", UnitCategory.LENGTH, 0.0254),
            UnitDefinition("nmi", "Nautical Mile", "nmi", UnitCategory.LENGTH, 1852.0)
        ),
        UnitCategory.WEIGHT to listOf(
            UnitDefinition("kg", "Kilogram", "kg", UnitCategory.WEIGHT, 1.0),
            UnitDefinition("g", "Gram", "g", UnitCategory.WEIGHT, 0.001),
            UnitDefinition("mg", "Milligram", "mg", UnitCategory.WEIGHT, 1e-6),
            UnitDefinition("ug", "Microgram", "µg", UnitCategory.WEIGHT, 1e-9),
            UnitDefinition("t", "Metric Tonne", "t", UnitCategory.WEIGHT, 1000.0),
            UnitDefinition("lb", "Pound", "lb", UnitCategory.WEIGHT, 0.45359237),
            UnitDefinition("oz", "Ounce", "oz", UnitCategory.WEIGHT, 0.028349523125),
            UnitDefinition("st", "Stone", "st", UnitCategory.WEIGHT, 6.35029318),
            UnitDefinition("ton_us", "US Short Ton", "ton", UnitCategory.WEIGHT, 907.18474)
        ),
        UnitCategory.TEMPERATURE to listOf(
            UnitDefinition("c", "Celsius", "°C", UnitCategory.TEMPERATURE),
            UnitDefinition("f", "Fahrenheit", "°F", UnitCategory.TEMPERATURE),
            UnitDefinition("k", "Kelvin", "K", UnitCategory.TEMPERATURE),
            UnitDefinition("r", "Rankine", "°R", UnitCategory.TEMPERATURE)
        ),
        UnitCategory.AREA to listOf(
            UnitDefinition("m2", "Square Meter", "m²", UnitCategory.AREA, 1.0),
            UnitDefinition("km2", "Square Kilometer", "km²", UnitCategory.AREA, 1_000_000.0),
            UnitDefinition("cm2", "Square Centimeter", "cm²", UnitCategory.AREA, 0.0001),
            UnitDefinition("mm2", "Square Millimeter", "mm²", UnitCategory.AREA, 1e-6),
            UnitDefinition("ha", "Hectare", "ha", UnitCategory.AREA, 10_000.0),
            UnitDefinition("ac", "Acre", "ac", UnitCategory.AREA, 4046.8564224),
            UnitDefinition("sqmi", "Square Mile", "mi²", UnitCategory.AREA, 2_589_988.110336),
            UnitDefinition("sqyd", "Square Yard", "yd²", UnitCategory.AREA, 0.83612736),
            UnitDefinition("sqft", "Square Foot", "ft²", UnitCategory.AREA, 0.09290304),
            UnitDefinition("sqin", "Square Inch", "in²", UnitCategory.AREA, 0.00064516)
        ),
        UnitCategory.VOLUME to listOf(
            UnitDefinition("l", "Liter", "L", UnitCategory.VOLUME, 1.0),
            UnitDefinition("ml", "Milliliter", "mL", UnitCategory.VOLUME, 0.001),
            UnitDefinition("m3", "Cubic Meter", "m³", UnitCategory.VOLUME, 1000.0),
            UnitDefinition("cm3", "Cubic Centimeter", "cm³", UnitCategory.VOLUME, 0.001),
            UnitDefinition("gal_us", "US Gallon", "gal", UnitCategory.VOLUME, 3.785411784),
            UnitDefinition("qt_us", "US Quart", "qt", UnitCategory.VOLUME, 0.946352946),
            UnitDefinition("pt_us", "US Pint", "pt", UnitCategory.VOLUME, 0.473176473),
            UnitDefinition("cup_us", "US Cup", "cup", UnitCategory.VOLUME, 0.2365882365),
            UnitDefinition("floz_us", "US Fluid Ounce", "fl oz", UnitCategory.VOLUME, 0.0295735295625),
            UnitDefinition("gal_uk", "Imperial Gallon", "gal (UK)", UnitCategory.VOLUME, 4.54609)
        ),
        UnitCategory.SPEED to listOf(
            UnitDefinition("mps", "Meters per Second", "m/s", UnitCategory.SPEED, 1.0),
            UnitDefinition("kmh", "Kilometers per Hour", "km/h", UnitCategory.SPEED, 1.0 / 3.6),
            UnitDefinition("mph", "Miles per Hour", "mph", UnitCategory.SPEED, 0.44704),
            UnitDefinition("kn", "Knot", "kn", UnitCategory.SPEED, 1852.0 / 3600.0),
            UnitDefinition("fps", "Feet per Second", "ft/s", UnitCategory.SPEED, 0.3048),
            UnitDefinition("mach", "Mach (Std Atmos)", "Ma", UnitCategory.SPEED, 340.29)
        ),
        UnitCategory.TIME to listOf(
            UnitDefinition("s", "Second", "s", UnitCategory.TIME, 1.0),
            UnitDefinition("ms", "Millisecond", "ms", UnitCategory.TIME, 0.001),
            UnitDefinition("us", "Microsecond", "µs", UnitCategory.TIME, 1e-6),
            UnitDefinition("ns", "Nanosecond", "ns", UnitCategory.TIME, 1e-9),
            UnitDefinition("min", "Minute", "min", UnitCategory.TIME, 60.0),
            UnitDefinition("h", "Hour", "h", UnitCategory.TIME, 3600.0),
            UnitDefinition("d", "Day", "d", UnitCategory.TIME, 86_400.0),
            UnitDefinition("wk", "Week", "wk", UnitCategory.TIME, 604_800.0),
            UnitDefinition("yr", "Julian Year", "yr", UnitCategory.TIME, 31_557_600.0)
        ),
        UnitCategory.DATA_STORAGE to listOf(
            UnitDefinition("b_si", "Byte", "B", UnitCategory.DATA_STORAGE, 1.0),
            UnitDefinition("kb", "Kilobyte (10³)", "kB", UnitCategory.DATA_STORAGE, 1_000.0),
            UnitDefinition("mb", "Megabyte (10⁶)", "MB", UnitCategory.DATA_STORAGE, 1_000_000.0),
            UnitDefinition("gb", "Gigabyte (10⁹)", "GB", UnitCategory.DATA_STORAGE, 1_000_000_000.0),
            UnitDefinition("tb", "Terabyte (10¹²)", "TB", UnitCategory.DATA_STORAGE, 1_000_000_000_000.0),
            UnitDefinition("pb", "Petabyte (10¹⁵)", "PB", UnitCategory.DATA_STORAGE, 1e15),
            UnitDefinition("bit_si", "Bit", "b", UnitCategory.DATA_STORAGE, 0.125),
            UnitDefinition("kbit", "Kilobit", "kb", UnitCategory.DATA_STORAGE, 125.0),
            UnitDefinition("mbit", "Megabit", "Mb", UnitCategory.DATA_STORAGE, 125_000.0),
            UnitDefinition("gbit", "Gigabit", "Gb", UnitCategory.DATA_STORAGE, 125_000_000.0)
        ),
        UnitCategory.PRESSURE to listOf(
            UnitDefinition("pa", "Pascal", "Pa", UnitCategory.PRESSURE, 1.0),
            UnitDefinition("kpa", "Kilopascal", "kPa", UnitCategory.PRESSURE, 1000.0),
            UnitDefinition("mpa", "Megapascal", "MPa", UnitCategory.PRESSURE, 1_000_000.0),
            UnitDefinition("bar", "Bar", "bar", UnitCategory.PRESSURE, 100_000.0),
            UnitDefinition("mbar", "Millibar / hPa", "mbar", UnitCategory.PRESSURE, 100.0),
            UnitDefinition("atm", "Standard Atmosphere", "atm", UnitCategory.PRESSURE, 101_325.0),
            UnitDefinition("psi", "Pounds per Sq Inch", "psi", UnitCategory.PRESSURE, 6894.757293168361),
            UnitDefinition("mmhg", "Millimeter of Mercury", "mmHg", UnitCategory.PRESSURE, 133.322387415)
        ),
        UnitCategory.ENERGY to listOf(
            UnitDefinition("j", "Joule", "J", UnitCategory.ENERGY, 1.0),
            UnitDefinition("kj", "Kilojoule", "kJ", UnitCategory.ENERGY, 1000.0),
            UnitDefinition("mj", "Megajoule", "MJ", UnitCategory.ENERGY, 1_000_000.0),
            UnitDefinition("cal", "Thermochemical Calorie", "cal", UnitCategory.ENERGY, 4.184),
            UnitDefinition("kcal", "Kilocalorie", "kcal", UnitCategory.ENERGY, 4184.0),
            UnitDefinition("wh", "Watt-hour", "Wh", UnitCategory.ENERGY, 3600.0),
            UnitDefinition("kwh", "Kilowatt-hour", "kWh", UnitCategory.ENERGY, 3_600_000.0),
            UnitDefinition("ev", "Electronvolt", "eV", UnitCategory.ENERGY, 1.602176634e-19),
            UnitDefinition("btu", "British Thermal Unit (IT)", "BTU", UnitCategory.ENERGY, 1055.05585262)
        ),
        UnitCategory.POWER to listOf(
            UnitDefinition("w", "Watt", "W", UnitCategory.POWER, 1.0),
            UnitDefinition("kw", "Kilowatt", "kW", UnitCategory.POWER, 1000.0),
            UnitDefinition("mw", "Megawatt", "MW", UnitCategory.POWER, 1_000_000.0),
            UnitDefinition("gw", "Gigawatt", "GW", UnitCategory.POWER, 1_000_000_000.0),
            UnitDefinition("hp", "Mechanical Horsepower", "hp", UnitCategory.POWER, 745.6998715822702),
            UnitDefinition("btuh", "BTU per Hour", "BTU/h", UnitCategory.POWER, 1055.05585262 / 3600.0)
        ),
        UnitCategory.ANGLE to listOf(
            UnitDefinition("deg", "Degree", "°", UnitCategory.ANGLE, 1.0),
            UnitDefinition("rad", "Radian", "rad", UnitCategory.ANGLE, 180.0 / PI),
            UnitDefinition("grad", "Gradian", "gon", UnitCategory.ANGLE, 0.9),
            UnitDefinition("arcmin", "Arcminute", "′", UnitCategory.ANGLE, 1.0 / 60.0),
            UnitDefinition("arcsec", "Arcsecond", "″", UnitCategory.ANGLE, 1.0 / 3600.0),
            UnitDefinition("rev", "Revolution", "rev", UnitCategory.ANGLE, 360.0)
        ),
        UnitCategory.FREQUENCY to listOf(
            UnitDefinition("hz", "Hertz", "Hz", UnitCategory.FREQUENCY, 1.0),
            UnitDefinition("khz", "Kilohertz", "kHz", UnitCategory.FREQUENCY, 1_000.0),
            UnitDefinition("mhz", "Megahertz", "MHz", UnitCategory.FREQUENCY, 1_000_000.0),
            UnitDefinition("ghz", "Gigahertz", "GHz", UnitCategory.FREQUENCY, 1_000_000_000.0),
            UnitDefinition("rpm", "Revolutions per Min", "rpm", UnitCategory.FREQUENCY, 1.0 / 60.0)
        ),
        UnitCategory.FUEL_CONSUMPTION to listOf(
            UnitDefinition("kml", "Kilometers per Liter", "km/L", UnitCategory.FUEL_CONSUMPTION),
            UnitDefinition("l100km", "Liters per 100 km", "L/100km", UnitCategory.FUEL_CONSUMPTION),
            UnitDefinition("mpg_us", "Miles per Gallon (US)", "mpg (US)", UnitCategory.FUEL_CONSUMPTION),
            UnitDefinition("mpg_uk", "Miles per Gallon (UK)", "mpg (UK)", UnitCategory.FUEL_CONSUMPTION)
        ),
        UnitCategory.DIGITAL_STORAGE to listOf(
            UnitDefinition("b_iec", "Byte", "B", UnitCategory.DIGITAL_STORAGE, 1.0),
            UnitDefinition("kib", "Kibibyte (2¹⁰)", "KiB", UnitCategory.DIGITAL_STORAGE, 1024.0),
            UnitDefinition("mib", "Mebibyte (2²⁰)", "MiB", UnitCategory.DIGITAL_STORAGE, 1024.0 * 1024.0),
            UnitDefinition("gib", "Gibibyte (2³⁰)", "GiB", UnitCategory.DIGITAL_STORAGE, 1024.0 * 1024.0 * 1024.0),
            UnitDefinition("tib", "Tebibyte (2⁴⁰)", "TiB", UnitCategory.DIGITAL_STORAGE, 1099511627776.0),
            UnitDefinition("pib", "Pebibyte (2⁵⁰)", "PiB", UnitCategory.DIGITAL_STORAGE, 1125899906842624.0),
            UnitDefinition("nibble", "Nibble", "nibble", UnitCategory.DIGITAL_STORAGE, 0.5),
            UnitDefinition("bit_iec", "Bit", "bit", UnitCategory.DIGITAL_STORAGE, 0.125),
            UnitDefinition("kibit", "Kibibit", "Kibit", UnitCategory.DIGITAL_STORAGE, 128.0),
            UnitDefinition("mibit", "Mebibit", "Mibit", UnitCategory.DIGITAL_STORAGE, 131_072.0),
            UnitDefinition("gibit", "Gibibit", "Gibit", UnitCategory.DIGITAL_STORAGE, 134_217_728.0)
        )
    )

    val allUnits: List<UnitDefinition> by lazy {
        unitsByCategory.values.flatten()
    }

    fun findUnitById(id: String): UnitDefinition? = allUnits.find { it.id == id }

    fun convert(value: Double, from: UnitDefinition, to: UnitDefinition): Double {
        if (from.category != to.category) return Double.NaN
        if (from.id == to.id) return value

        return when (from.category) {
            UnitCategory.TEMPERATURE -> convertTemperature(value, from.id, to.id)
            UnitCategory.FUEL_CONSUMPTION -> convertFuel(value, from.id, to.id)
            else -> (value * from.toBaseFactor) / to.toBaseFactor
        }
    }

    private fun convertTemperature(value: Double, fromId: String, toId: String): Double {
        val celsius = when (fromId) {
            "c" -> value
            "f" -> (value - 32.0) * 5.0 / 9.0
            "k" -> value - 273.15
            "r" -> (value - 491.67) * 5.0 / 9.0
            else -> return Double.NaN
        }
        return when (toId) {
            "c" -> celsius
            "f" -> celsius * 9.0 / 5.0 + 32.0
            "k" -> celsius + 273.15
            "r" -> (celsius + 273.15) * 9.0 / 5.0
            else -> Double.NaN
        }
    }

    private fun convertFuel(value: Double, fromId: String, toId: String): Double {
        if (value <= 0.0) return 0.0
        // Base unit: km/L
        val kmPerLiter = when (fromId) {
            "kml" -> value
            "l100km" -> 100.0 / value
            "mpg_us" -> value * (1.609344 / 3.785411784)
            "mpg_uk" -> value * (1.609344 / 4.54609)
            else -> return Double.NaN
        }
        return when (toId) {
            "kml" -> kmPerLiter
            "l100km" -> 100.0 / kmPerLiter
            "mpg_us" -> kmPerLiter / (1.609344 / 3.785411784)
            "mpg_uk" -> kmPerLiter / (1.609344 / 4.54609)
            else -> Double.NaN
        }
    }
}
