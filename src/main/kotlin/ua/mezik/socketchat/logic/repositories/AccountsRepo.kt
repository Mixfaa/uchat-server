package ua.mezik.socketchat.logic.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ua.mezik.socketchat.models.Account

@Repository
interface AccountsRepo : JpaRepository<Account, Long> {
    fun findByUsernameAndPassword(username: String, password: String): Account?
    fun findByUsername(username: String): Account?
    fun findAllByUsernameContainingIgnoreCase(username: String, page: Pageable): Page<Account>
}