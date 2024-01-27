package ua.mezik.socketchat.model

import ua.mezik.socketchat.handling.ClientHandler

data class PersistedConnection(var account: Account, var clients: MutableList<ClientHandler>)