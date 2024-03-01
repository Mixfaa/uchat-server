package com.mezik.uchat

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.io.FileInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.TrustManagerFactory


@Configuration
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@EnableReactiveMongoRepositories
@EnableScheduling
class SocketChat {
    @Bean
    fun sslContextBean(
        @Value("\${keystore.path}") keyStorePath: String,
        @Value("\${keystore.password}") keyStorePassword: String
    ): SSLContext {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileInputStream(keyStorePath), keyStorePassword.toCharArray())

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray())

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())

        return sslContext
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun sslServerSocketFactory(sslContext: SSLContext): SSLServerSocketFactory {
        return sslContext.serverSocketFactory
    }

}

fun main(args: Array<String>) {
    Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
    runApplication<SocketChat>(*args)
}