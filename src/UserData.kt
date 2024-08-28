data class UserData(val name: String, val address: String, val service: String) {
    fun toArray(): Array<Object> {
        return arrayOf(name, address, service) as Array<Object>
    }
}