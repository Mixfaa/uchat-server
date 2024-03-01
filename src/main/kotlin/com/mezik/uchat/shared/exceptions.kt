package com.mezik.uchat.shared

class NotFoundException(subject: String) : Throwable("$subject: not found")

class PasswordNotMatchException(message: String) : Throwable(message)

class AccessDeniedException(message: String) : Throwable(message)

class ValueNotProvidedException(message: String) : Throwable(message)

class MessageNotEditableException(message: String) : Throwable(message)

class UsernameTakenException(username: String) : Throwable("Username $username is already taken")

object CachedExceptions {
    val passwordNotMatch = PasswordNotMatchException("Password does not match")
    val accessDenied = AccessDeniedException("Access denied")
    val publicKeyNotProvided = ValueNotProvidedException("Public key not provided")
    val messageNotEditable = MessageNotEditableException("Message not editable")
}