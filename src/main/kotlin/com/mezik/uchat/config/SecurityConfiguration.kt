package com.mezik.uchat.config

import com.mezik.uchat.service.AccountsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    private val accountsService: AccountsService,
    private val passwordEncoder: PasswordEncoder
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf(CsrfConfigurer<HttpSecurity>::disable)
        http.authorizeHttpRequests { customizer ->
            customizer.requestMatchers("/static/**").permitAll()
            customizer.requestMatchers("/rest_interface/register").permitAll()
            customizer.requestMatchers("/rest_interface/auth").authenticated()
        }
        http.httpBasic(Customizer.withDefaults())
        return http.build()
    }


    @Autowired
    fun configureSecurity(auth: AuthenticationManagerBuilder) {
        val authProvider = DaoAuthenticationProvider(passwordEncoder)
        authProvider.setUserDetailsService(accountsService)

        auth.authenticationProvider(authProvider)
    }
}