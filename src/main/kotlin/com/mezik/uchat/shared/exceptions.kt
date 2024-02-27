package com.mezik.uchat.shared

class EmptyMonoException(message: String) : Throwable(message)

class PasswordNotMatchException(message: String) : Throwable(message)

class AccessDeniedException(message: String) : Throwable(message)

class ValueNotProvidedException(message: String) : Throwable(message)

class MessageNotEditableException(message: String) : Throwable(message)

class UsernameTakenException(message: String) : Throwable(message)

object CachedExceptions {
    val emptyMono = EmptyMonoException("No elements")
    val passwordNotMatch = PasswordNotMatchException("Password does not matches")
    val accessDenied = AccessDeniedException("Access denied")
    val publicKeyNotProvided = ValueNotProvidedException("Public key not provided")
    val messageNotEditable = MessageNotEditableException("Message not editable")
}