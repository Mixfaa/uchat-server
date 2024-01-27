package ua.mezik.socketchat.model

import ua.mezik.socketchat.handling.ChatClient

data class PersistedConnection(var account: Account, var clients: MutableList<ChatClient>)