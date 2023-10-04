package com.toyproject.instagram.security;

import com.toyproject.instagram.entity.User;
import com.toyproject.instagram.repository.UserMapper;
import com.toyproject.instagram.service.PrincipalDetailsService;
import com.toyproject.instagram.service.UserService;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;

// JWT 토큰을 관리해주는 로직
@Component
public class JwtTokenProvider {
    private final Key key;
    private final PrincipalDetailsService principalDetailsService;
    private final UserMapper userMapper;

    // Autowired는 IoC 컨테이너에서 객체를 자동 주입
    // Value는 application.yml에서 변수 데이터를 자동 주입

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Autowired PrincipalDetailsService principalDetailsService,
                            @Autowired UserMapper userMapper) {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.principalDetailsService = principalDetailsService;
        this.userMapper = userMapper;
    }

    // JWT 토큰을 생성
    public String generateAccessToken(Authentication authentication) {
        PrincipalUser principalUser = (PrincipalUser) authentication.getPrincipal();    // 다운캐스팅

        Date tokenExpiresDate = new Date(new Date().getTime() + (1000 * 60 * 60 * 24));
        // 현재 시간에서 한 시간 뒤 만료 (1000: 1초, *60: 1분, *60: 1시간, *24: 1일)

        JwtBuilder jwtBuilder = Jwts.builder()    // JWT 생성하기 위한 빌더 객체
                .setSubject("AccessToken")  // 토큰의 제목
                .setExpiration(tokenExpiresDate)    // 만료 기간
                .signWith(key, SignatureAlgorithm.HS256);    // JWT 서명(서명에 사용할 키, 알고리즘)

        // phone, email이면 db에서 찾아옴
        User user= userMapper.findUserByUsername(principalUser.getUsername());
        if(user != null) {
            return jwtBuilder.claim("username", user.getUsername()).compact();
        }
        user= userMapper.findUserByEmail(principalUser.getUsername());
        if(user != null) {
            return jwtBuilder.claim("username", user.getUsername()).compact();
        }

        //phone, email 아니면 username - db에서 가져올 필요 없음.
        return jwtBuilder.claim("username", principalUser.getUsername()).compact();
    }

    // 토큰이 유효한지 검사하는 메소드
    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key) // 서명 키 설정
                    .build()
                    .parseClaimsJws(token); // 주어진 토큰을 파싱하고 서명을 확인
        } catch (Exception e) {
            return false;   // 예외 발생 -> 토큰이 유효하지 않음
        }
        return true;
    }

    public String convertToken(String bearerToken) {
        String type = "Bearer ";
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(type)) {  // hasText : null이 아니고 비어 있지 않은 문자열이면 참.
            return bearerToken.substring(type.length());    // 'Bearer'을 제외한 토큰만 자름.
        }
        return "";
    }

    public Authentication getAuthentication(String accessToken) {
        Authentication authentication = null;

        // JWT 토큰 안에 담긴 username을 추출
        String username = Jwts
                .parserBuilder()
                .setSigningKey(key) // 토큰을 검증하기 위한 서명 키 설정
                .build()
                .parseClaimsJws(accessToken) // 토큰 파싱
                .getBody() // 토큰의 본문(Claims)을 가져옴
                .get("username") // "username" 키에 해당하는 값을 추출
                .toString();

        // username을 사용하여 사용자 정보를 데이터베이스에서 가져옴
        PrincipalUser principalUser = (PrincipalUser) principalDetailsService.loadUserByUsername(username);

        // 사용자 정보를 기반으로 Authentication 객체 생성
        // UsernamePasswordAuthenticationToken은 사용자 정보와 권한을 포함하는 Authentication 객체
        authentication = new UsernamePasswordAuthenticationToken(principalUser, null, principalUser.getAuthorities());

        return authentication;
    }
}
