package com.mezik.uchat.model.database

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mezik.uchat.model.message.PublicKey
import com.mezik.uchat.service.SequencedIdGenerator
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Document("account")
data class Account(
    @JvmField val username: String,
    @JvmField @field:JsonIgnore val password: String,
    val publicKey: PublicKey,
    @field:JsonIgnore @Id val databaseId: ObjectId = ObjectId(),
    val id: Long = SequencedIdGenerator.instance.generateId("account")
) : UserDetails {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (databaseId != other.databaseId) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = databaseId.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    override fun toString(): String {
        return "Account(username='$username', databaseId=$databaseId, id=$id)"
    }

    @JsonIgnore
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = AUTHORITIES

    override fun getUsername(): String = username

    @JsonIgnore
    override fun getPassword(): String = password

    @JsonIgnore
    override fun isAccountNonExpired(): Boolean = true

    @JsonIgnore
    override fun isAccountNonLocked(): Boolean = true

    @JsonIgnore
    override fun isCredentialsNonExpired(): Boolean = true

    @JsonIgnore
    override fun isEnabled(): Boolean = true

    companion object {
        private val AUTHORITIES = mutableListOf(object : GrantedAuthority {
            override fun getAuthority(): String = "user"
        })
    }
}