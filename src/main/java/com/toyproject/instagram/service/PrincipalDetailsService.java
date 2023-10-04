package com.toyproject.instagram.service;

import com.toyproject.instagram.entity.User;
import com.toyproject.instagram.repository.UserMapper;
import com.toyproject.instagram.security.PrincipalUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    // authenticationManager가 loadUserByUsername()를 호출
    // UserDatails가 리턴 타입 - null을 반환할 시 예외 던져줌 --> 매니저가 예외 처리 -> 호출한 지점에서 예외처리가 됨
    @Override
    public UserDetails loadUserByUsername(String phoneOrEmailOrUsername) throws UsernameNotFoundException {
        User user = userMapper.findUserByPhone(phoneOrEmailOrUsername);
        if(user != null) {
            return new PrincipalUser(user.getPhone(), user.getPassword(), user.getAuthorities());
        }

        user = userMapper.findUserByEmail(phoneOrEmailOrUsername);
        if(user != null) {
            return new PrincipalUser(user.getEmail(), user.getPassword(), user.getAuthorities());
        }

        user = userMapper.findUserByUsername(phoneOrEmailOrUsername);
        if(user != null) {
            return new PrincipalUser(user.getUsername(), user.getPassword(), user.getAuthorities());
        }

        throw new UsernameNotFoundException("잘못된 사용자 정보입니다. 다시 확인하세요.");
    }
}
