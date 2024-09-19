package com.pa.evs.security.jwt;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.pa.evs.dto.SettingDto;
import com.pa.evs.model.Login;
import com.pa.evs.repository.LoginRepository;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.SettingService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClock;

@Component
public class JwtTokenUtil implements Serializable {

    static final String CLAIM_KEY_USERNAME = "sub";
    static final String CLAIM_KEY_CREATED = "iat";
    private static final long serialVersionUID = -3301605591108950415L;
    private Clock clock = DefaultClock.INSTANCE;
    
    @Autowired SettingService settingService;
    
    @Autowired LoginRepository loginRepository;

    @Value("${jwt.secret}")
    private String secret;

    public Long getExpiration() {
    	SettingDto dto = settingService.findByKey("TIME_LOGIN_EXPIRED");
    	if (dto == null || dto.getValue() == null || !dto.getValue().matches("^[0-9]+$")) {
    		return 3600l;
    	}
    	return Long.valueOf(dto.getValue());
    };

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getIssuedAtDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaimsFromToken(String token) {
    	if (token != null && token.startsWith("Bearer ")) {
    		token = token.substring(7);
    	}
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(clock.now());
    }

    private Boolean isCreatedBeforeLastPasswordReset(Date created, Date lastPasswordReset) {
        return (lastPasswordReset != null && created.before(lastPasswordReset));
    }

    private Boolean ignoreTokenExpiration(String token) {
        // here you specify tokens, for that the expiration is ignored
        return false;
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return doGenerateToken(claims, userDetails.getUsername());
    }

    public String doGenerateToken(Map<String, Object> claims, String subject) {
        final Date createdDate = clock.now();
        final Date expirationDate = claims.get("tokenExpireDate") != null ? (Date) claims.get("tokenExpireDate") : calculateExpirationDate(createdDate);
        String tokenId = UUID.randomUUID().toString();
        
        Login login = new Login();
        login.setTokenId(tokenId);
        login.setStartTime(createdDate.getTime());
        login.setEndTime(expirationDate.getTime());
        login.setUserName(subject);
        loginRepository.save(login);
        
        return Jwts.builder()
            .setClaims(claims)
            .setAudience(tokenId)
            .setSubject(subject)
            .setIssuedAt(createdDate)
            .setExpiration(expirationDate)
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }

    public Boolean canTokenBeRefreshed(String token, Date lastPasswordReset) {
        final Date created = getIssuedAtDateFromToken(token);
        return !isCreatedBeforeLastPasswordReset(created, lastPasswordReset)
            && (!isTokenExpired(token) || ignoreTokenExpiration(token));
    }

    public String refreshToken(String token) {
        final Date createdDate = clock.now();
        final Date expirationDate = calculateExpirationDate(createdDate);

        final Claims claims = getAllClaimsFromToken(token);
        claims.setIssuedAt(createdDate);
        claims.setExpiration(expirationDate);

        return Jwts.builder()
            .setClaims(claims)
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        JwtUser user = (JwtUser) userDetails;
        final String username = getUsernameFromToken(token);
        Claims claims = getAllClaimsFromToken(token);
        final String tokenId = claims.getAudience();
        
        Optional<Login> loginOpt = loginRepository.findByTokenIdAndUserName(tokenId, username);
        
        if (!loginOpt.isPresent()) {
        	return false;
        }
        
        Login login = loginOpt.get();
        Boolean isTokenExpr = login.getEndTime() <= System.currentTimeMillis() || isTokenExpired(token);
        
        return (user != null && tokenId != null && username.equals(user.getUsername()) && !isTokenExpr);
    }

    private Date calculateExpirationDate(Date createdDate) {
        return new Date(createdDate.getTime() + getExpiration() * 1000);
    }
}
