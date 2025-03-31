package com.repo.security.core.jwt.provider

import com.repo.security.common.exception.SecurityException.JwtException.*
import com.repo.security.core.config.JwtConfig
import com.repo.security.core.jwt.enums.JwtHeaders.*
import com.repo.security.core.jwt.model.JwtRequestDto
import com.repo.security.domain.user.enums.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtProvider(
    private val config: JwtConfig,
) {
    private val key by lazy { Keys.hmacShaKeyFor(config.secret.toByteArray()) }

    fun generateToken(
        dto: JwtRequestDto
    ): String {
        val now = Date()
        val expiry = Date(now.time + config.expiration)

        return Jwts.builder()
            .subject(dto.id)
            .claim(ROLE.header, dto.role.role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun validateToken(
        token: String,
        expectedRole: UserRole
    ): Boolean {
        return runCatching {
            val claims = this.getClaims(token)

            val isExpired = claims.expiration.before(Date())
            if (isExpired) throw ExpiredTokenException()

            val roleInToken = claims[ROLE.header] as? String
            if (roleInToken == null || roleInToken != expectedRole.role) throw InvalidRoleException()

            true
        }.getOrElse {
            throw it
        }
    }

    fun getIdByToken(
        token: String
    ): String =
        this.getClaims(token).subject

    private fun getClaims(token: String) =
        runCatching {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        }.getOrElse {
            throw InvalidTokenException()
        }

}