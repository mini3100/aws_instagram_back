package com.toyproject.instagram.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // HttpServletRequest로 형변환하여 요청과 토큰을 가져옴
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader("Authorization");
        String jwtToken = jwtTokenProvider.convertToken(token);
        String uri = httpServletRequest.getRequestURI();

        // 인증 절차
        // URI가 "api/v1/auth"로 시작하지 않고(=로그인, 회원가입 외) JWT 토큰이 유효한 경우
        if (!uri.startsWith("api/v1/auth") && jwtTokenProvider.validateToken(jwtToken)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(jwtToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 시큐리티 인증 상태에 Authentication 객체가 존재하면 인증된 상태임을 의미
        }

        // 다음 필터로 체인을 전달하여 요청 처리를 계속 진행
        chain.doFilter(request, response);
    }
}

