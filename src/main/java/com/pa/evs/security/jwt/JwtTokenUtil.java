package com.pa.evs.security.jwt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.pa.evs.dto.SettingDto;
import com.pa.evs.model.Login;
import com.pa.evs.repository.LoginRepository;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.SettingService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClock;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenUtil implements Serializable {

    static final String CLAIM_KEY_USERNAME = "sub";
    static final String CLAIM_KEY_CREATED = "iat";
    private static final long serialVersionUID = -3301605591108950415L;
    private Clock clock = DefaultClock.INSTANCE;
    
    @Autowired SettingService settingService;
    
    @Autowired LoginRepository loginRepository;

//    @Value("${jwt.secret}")
    private String secret = "3301605591108950415L3301605591108950415L";

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
    
    public <T> T getClaimFromTokenAudience(String token, Function<Claims, Collection<T>> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        List<T> lst = new ArrayList<>(claimsResolver.apply(claims));
        return lst.isEmpty() ? null : lst.get(0);
    }

    public Claims getAllClaimsFromToken(String token) {
    	if (token != null && token.startsWith("Bearer ")) {
    		token = token.substring(7);
    	}
    	JwtParser jwtParser = Jwts.parser()
    		    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
    		    .build();
    	return (Claims) jwtParser.parse(token).getPayload();
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
        
        claims.put("tokenId", tokenId);
        claims.put("subject", subject);
        claims.put("timestamp", login.getStartTime());
        
        return Jwts.builder()
                .claims(claims)
                .audience().add(tokenId).and()
                .subject(subject)
                .issuedAt(createdDate)
                .expiration(expirationDate)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), Jwts.SIG.HS256)
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

        return Jwts.builder()
                .claims(claims)
                .issuedAt(createdDate)
                .expiration(expirationDate)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), Jwts.SIG.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        JwtUser user = (JwtUser) userDetails;
        final String username = getUsernameFromToken(token);
        Claims claims = getAllClaimsFromToken(token);
        final String tokenId =  (String) claims.get("tokenId");
        final Long timestamp =  (Long) claims.get("timestamp");
        
        Optional<Login> loginOpt = loginRepository.findByTokenIdAndUserName(tokenId, username);
        
        if (!loginOpt.isPresent()) {
        	return false;
        }
        
        Login login = loginOpt.get();
        Boolean isTokenExpr = login.getEndTime() <= System.currentTimeMillis() || isTokenExpired(token);
        boolean rs = (user != null && tokenId != null && username.equals(user.getUsername()) && !isTokenExpr);
        if (userDetails instanceof JwtUser) {
        	((JwtUser) userDetails).setTokenId(tokenId);
        	((JwtUser) userDetails).setTokenCreatedDate(timestamp);
        }
        return rs;
    }

    private Date calculateExpirationDate(Date createdDate) {
        return new Date(createdDate.getTime() + getExpiration() * 1000);
    }
}
