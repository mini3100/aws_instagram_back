package com.toyproject.instagram.config;

import com.toyproject.instagram.exception.AuthenticateExceptionEntryPoint;
import com.toyproject.instagram.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity  // 현재 우리가 만든 Security 설정 정책을 따르겠다.
@Configuration      // 설정 Component
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final AuthenticateExceptionEntryPoint authenticateExceptionEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean   // 외부 메소드에 붙이면 IoC에 등록 된다
    public BCryptPasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder : 외부 메소드 -> 코드를 수정할 수 없기 때문에 어노테이션을 달 수 없다. Bean을 붙이면서 IoC에 등록
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors();    // WebMvcConfig에서 설정한 Cors 정책을 따르겠다.
        http.csrf().disable();  // csrf 토큰 비활성화

        http.authorizeRequests()
                .antMatchers("/api/v1/auth/**")     // /api/v1/auth 로 시작하는 모든 요청(로그인, 회원가입)
                .permitAll()                        // 인증 없이 요청을 허용하겠다.
                .anyRequest()                       // 나머지 모든 요청은
                .authenticated()                    // 인증을 받아라
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(authenticateExceptionEntryPoint);
    }
}
