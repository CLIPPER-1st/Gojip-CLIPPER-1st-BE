package com.example.gojipserver.domain.oauth2.service;

import com.example.gojipserver.domain.oauth2.dto.KakaoUserInfoDto.*;
import com.example.gojipserver.domain.oauth2.dto.UserDto;
import com.example.gojipserver.domain.oauth2.dto.UserDto.*;
import com.example.gojipserver.domain.user.entity.Role;
import com.example.gojipserver.domain.user.entity.User;
import com.example.gojipserver.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoService{
    private final UserRepository userRepository;
    private final WebClient webClient;

    public UserInfoDto getUserProfileByToken(String accessToken){
        KakaoUserInfoResponse kakaoInfoDto = getUserAttributesByToken(accessToken);
        User newUser = User.builder()
                .id(kakaoInfoDto.getId())
                .email(kakaoInfoDto.getKakao_account().getEmail())
                .nickname(kakaoInfoDto.getProperties().getNickname())
                .role(Role.GUEST)
                .build();
        log.info("kakaoUserId : {}", kakaoInfoDto.getId());
        if(userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            User user = userRepository.findByEmail(newUser.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
            user.update(kakaoInfoDto.getKakao_account().getEmail(), kakaoInfoDto.getProperties().getNickname());
        } else {
            userRepository.save(newUser);
        }
        UserInfoDto userDto = UserInfoDto.builder()
                .id(newUser.getId())
                .email(newUser.getEmail())
                .build();
        return userDto;
    }
    public KakaoUserInfoResponse getUserAttributesByToken(String accessToken){
        Flux<KakaoUserInfoResponse> response = webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToFlux(KakaoUserInfoResponse.class);
        return response.blockFirst();
    }
}