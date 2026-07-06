package com.sungjiduk.backend.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // User쪽
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "검증에 실패했습니다."),
    USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "잘못된 비밀번호 또는 존재하지 않는 이메일입니다."),
    USER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 계정입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "해당 Refresh Token은 만료되었습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 포함되어있지 않습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "현재 계정에 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 계정을 찾을 수 없습니다."),
    NOT_SAME_ORIGIN_PROVIDER(HttpStatus.BAD_REQUEST, "계정의 이메일 도메인과 다른 도메인에서 로그인을 시도했습니다."),
    NOT_REGISTERED(HttpStatus.NOT_FOUND, "가입되어 있지 않은 계정입니다."),
    AUTHENTICATION_ERROR(HttpStatus.UNAUTHORIZED, "인증 과정 중 오류가 발생했습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "해당 토큰은 만료된 토큰입니다."),
    ERROR_FROM_TOKEN(HttpStatus.UNAUTHORIZED, "토큰에서 문제가 발생했습니다."),
    ABNORMAL_TOKEN(HttpStatus.UNAUTHORIZED, "형식이 올바르지 않은 토큰입니다."),
    ACCOUNT_NOT_ADMIN(HttpStatus.UNAUTHORIZED, "해당 계정에 어드민 권한이 없습니다."),

    // Content쪽
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 작품을 찾을 수 없습니다."),

    // Spot쪽
    SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 성지를 찾을 수 없습니다."),
    ANITABI_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "Anitabi 성지 데이터를 불러오지 못했습니다."),

    ;
    private final HttpStatus status;
    private final String description;

}
