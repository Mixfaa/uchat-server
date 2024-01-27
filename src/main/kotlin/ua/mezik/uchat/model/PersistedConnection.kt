package ua.mezik.uchat.model

import ua.mezik.uchat.services.ChatClient

data class PersistedConnection(var account: Account, var clients: MutableList<ChatClient>)