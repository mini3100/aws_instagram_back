package com.toyproject.instagram.service;

import com.toyproject.instagram.dto.SigninReqDto;
import com.toyproject.instagram.dto.SignupReqDto;
import com.toyproject.instagram.entity.User;
import com.toyproject.instagram.exception.JwtException;
import com.toyproject.instagram.exception.SignupException;
import com.toyproject.instagram.repository.UserMapper;
import com.toyproject.instagram.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;

    public void signupUser(SignupReqDto signupReqDto) {
        User user = signupReqDto.toUserEntity(passwordEncoder);

        // 정규 표현식 패턴을 사용하여 패턴 객체 생성
        Matcher emailMatcher = Pattern.compile("^[a-zA-Z0-9]+@[0-9a-zA-Z]+\\.[a-z]*$").matcher(signupReqDto.getPhoneOrEmail());
        Matcher phoneMatcher = Pattern.compile("^[0-9]{11}+$").matcher(signupReqDto.getPhoneOrEmail());

        if(emailMatcher.matches()) {    // 이메일 패턴과 일치하면 이메일 주소 설정
            user.setEmail(signupReqDto.getPhoneOrEmail());
        }
        if(phoneMatcher.matches()) {    // 전화번호 패턴과 일치하면 전화번호 설정
            user.setPhone(signupReqDto.getPhoneOrEmail());
        }

        checkDuplicated(user);  // db에 저장하기 전 중복 체크

        Integer executeCount = userMapper.saveUser(user);
    }

    private void checkDuplicated(User user) {
        if(StringUtils.hasText(user.getPhone())) {
            if(userMapper.findUserByPhone(user.getPhone()) != null) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("phone", "이미 사용중인 연락처입니다.");
                throw new SignupException(errorMap);
            }
        }
        if(StringUtils.hasText(user.getEmail())) {
            if(userMapper.findUserByEmail(user.getEmail()) != null) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("email", "이미 사용중인 이메일입니다.");
                throw new SignupException(errorMap);
            }
        }
        if(StringUtils.hasText(user.getUsername())) {
            if(userMapper.findUserByUsername(user.getUsername()) != null) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("username", "이미 사용중인 사용자 이름입니다.");
                throw new SignupException(errorMap);
            }
        }
    }

    public String signinUser(SigninReqDto signinReqDto) {
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(signinReqDto.getPhoneOrEmailOrUsername(), signinReqDto.getLoginPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        return accessToken;
    }

    public Boolean authenticate(String token) {
        String accessToken = jwtTokenProvider.convertToken(token);
        if(!jwtTokenProvider.validateToken(accessToken)) {  // 토큰이 유효하지 않다면
            throw new JwtException("사용자 정보가 만료되었습니다. 다시 로그인하세요.");
        }
        return true;
    }
}
