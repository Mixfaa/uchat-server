package ua.mezik.uchat.model

data class PersistedConnection(var account: Account, var clients: MutableList<ChatClient>)