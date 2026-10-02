package com.example.myapplication


import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class User(
    val id: Long,
    val email: String,
    val passwordHash: String
)

data class Item(
    val id: Long,
    val userId: Long,
    val title: String,
    val description: String,
    val status: String,
    val createdAt: Long
)

class AppRepository(context: Context) {

    private val prefs =
        context.getSharedPreferences("app_data", Context.MODE_PRIVATE)

    private fun readUsers(): MutableList<User> {
        val result = mutableListOf<User>()
        val json = prefs.getString("users", "[]") ?: "[]"

        val array = JSONArray(json)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            result.add(
                User(
                    id = obj.getLong("id"),
                    email = obj.getString("email"),
                    passwordHash = obj.getString("passwordHash")
                )
            )
        }

        return result
    }

    private fun saveUsers(users: List<User>) {
        val array = JSONArray()

        users.forEach { user ->
            val obj = JSONObject()

            obj.put("id", user.id)
            obj.put("email", user.email)
            obj.put("passwordHash", user.passwordHash)

            array.put(obj)
        }

        prefs.edit()
            .putString("users", array.toString())
            .apply()
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String
    ): String? {

        val cleanEmail = email.trim()

        if (cleanEmail.isEmpty()) {
            return "E-posta boş bırakılamaz."
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return "Geçerli bir e-posta adresi girin."
        }

        if (password.length < 6) {
            return "Şifre en az 6 karakter olmalıdır."
        }

        if (password != confirmPassword) {
            return "Şifreler eşleşmiyor."
        }

        val users = readUsers()

        if (users.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return "Bu e-posta zaten kayıtlı."
        }

        val newUser = User(
            id = System.currentTimeMillis(),
            email = cleanEmail,
            passwordHash = hashPassword(password)
        )

        users.add(newUser)

        saveUsers(users)

        return null
    }

    fun login(
        email: String,
        password: String
    ): String? {

        val cleanEmail = email.trim()

        if (cleanEmail.isEmpty() || password.isEmpty()) {
            return null
        }

        val users = readUsers()

        val user = users.find {
            it.email.equals(cleanEmail, ignoreCase = true) &&
                    it.passwordHash == hashPassword(password)
        }

        if (user != null) {
            prefs.edit()
                .putLong("session_user_id", user.id)
                .apply()

            return user.id.toString()
        }

        return null
    }

    fun getSessionUserId(): Long? {
        val value = prefs.getLong("session_user_id", -1L)

        return if (value == -1L) {
            null
        } else {
            value
        }
    }

    fun logout() {
        prefs.edit()
            .remove("session_user_id")
            .apply()
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }

   // Items

    private fun readItems(): MutableList<Item> {

        val result = mutableListOf<Item>()

        val json = prefs.getString("items", "[]") ?: "[]"

        val array = JSONArray(json)

        for (i in 0 until array.length()) {

            val obj = array.getJSONObject(i)

            result.add(
                Item(
                    id = obj.getLong("id"),
                    userId = obj.getLong("userId"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    status = obj.getString("status"),
                    createdAt = obj.getLong("createdAt")
                )
            )
        }

        return result
    }

    private fun saveItems(items: List<Item>) {

        val array = JSONArray()

        items.forEach { item ->

            val obj = JSONObject()

            obj.put("id", item.id)
            obj.put("userId", item.userId)
            obj.put("title", item.title)
            obj.put("description", item.description)
            obj.put("status", item.status)
            obj.put("createdAt", item.createdAt)

            array.put(obj)
        }

        prefs.edit()
            .putString("items", array.toString())
            .apply()
    }

    fun getItems(userId: Long): List<Item> {
        return readItems()
            .filter { it.userId == userId }
    }

    fun getItem(
        userId: Long,
        itemId: Long
    ): Item? {

        return readItems().find {
            it.userId == userId && it.id == itemId
        }
    }

    fun addItem(
        userId: Long,
        title: String,
        description: String,
        status: String
    ) {

        val items = readItems()

        val item = Item(
            id = System.currentTimeMillis(),
            userId = userId,
            title = title,
            description = description,
            status = status,
            createdAt = System.currentTimeMillis()
        )

        items.add(item)

        saveItems(items)
    }

    fun updateItem(
        userId: Long,
        itemId: Long,
        title: String,
        description: String,
        status: String
    ): Boolean {

        val items = readItems()

        val index = items.indexOfFirst {
            it.id == itemId && it.userId == userId
        }

        if (index == -1) {
            return false
        }

        val oldItem = items[index]

        items[index] = oldItem.copy(
            title = title,
            description = description,
            status = status
        )

        saveItems(items)

        return true
    }

    fun deleteItem(
        userId: Long,
        itemId: Long
    ): Boolean {

        val items = readItems()

        val oldSize = items.size

        val newItems = items.filterNot {
            it.userId == userId && it.id == itemId
        }

        if (oldSize == newItems.size) {
            return false
        }

        saveItems(newItems)

        return true
    }
}