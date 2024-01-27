package ua.mezik.socketchat.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ua.mezik.socketchat.model.Account
import ua.mezik.socketchat.model.Chat

@Repository
interface ChatsRepo : JpaRepository<Chat, Long> {
    fun existsByName(name: String): Boolean
    fun findAllByName(name: String): Iterable<Chat>
    fun findAllByOwner(owner: Account, pageable: Pageable): Page<Chat>
    fun findAllByParticipantsContaining(participant: Account, pageable: Pageable): Page<Chat>

    @Query(
        "SELECT DISTINCT c.id FROM Chat c JOIN c.participants cp ON c.id = cp.id WHERE cp = :account"
    )
    fun findAllIdsByOwnerId(@Param("account") owner: Account): List<Long>

    @Query("SELECT DISTINCT c FROM Chat c JOIN c.participants p WHERE c.id IN :chatIds AND p = :participant")
    fun findAllByIdAndParticipantsContaining(@Param("chatIds") ids: Iterable<Long>,@Param("participant") participant: Account): Iterable<Chat>
}