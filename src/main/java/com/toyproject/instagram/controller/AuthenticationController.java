package com.toyproject.instagram.controller;

import com.toyproject.instagram.dto.SigninReqDto;
import com.toyproject.instagram.dto.SignupReqDto;
import com.toyproject.instagram.exception.SignupException;
import com.toyproject.instagram.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")  // 카카오, 배민 등 api 제작 회사에서 대부분 /api/v1(version1)을 붙인다. 라우트랑 헷갈려서 api라는 표기
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserService userService;

    @PostMapping("/user")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupReqDto signupReqDto, BindingResult bindingResult) {

        if(bindingResult.hasErrors()) { // 에러가 있으면
            Map<String, String> errorMap = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> {   // 에러 하나씩 꺼내서
                errorMap.put(error.getField(), error.getDefaultMessage());  // errorMap에 집어넣음
            });
            throw new SignupException(errorMap);    // 예외 처리
        }

        userService.signupUser(signupReqDto);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/login")   // 로그인 : Post 요청 url에 표시되는 데이터를 숨김
    public ResponseEntity<?> signin(@RequestBody SigninReqDto signinReqDto) {
        String accessToken = userService.signinUser(signinReqDto);
        return ResponseEntity.ok().body(accessToken);   // 정상적으로 로그인 됐을 경우 return
    }

    @GetMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestHeader(value = "Authorization") String token) {
        return ResponseEntity.ok(userService.authenticate(token));
    }
}
