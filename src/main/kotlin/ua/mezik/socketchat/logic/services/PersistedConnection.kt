package ua.mezik.socketchat.logic.services

import ua.mezik.socketchat.logic.ClientHandler
import ua.mezik.socketchat.models.Account

data class PersistedConnection(var account: Account, var clients: MutableList<ClientHandler>)