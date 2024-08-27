class UserData(val name: String, val address: String, val service: String) {
    fun toLegacyString(): String {
        return "$name¬$address¬$service"
    }

    fun toArray(): Array<Object> {
        return arrayOf(name, address, service) as Array<Object>
    }
}