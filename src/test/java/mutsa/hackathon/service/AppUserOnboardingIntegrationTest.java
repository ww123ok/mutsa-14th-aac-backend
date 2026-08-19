package mutsa.hackathon.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.MeResponse;
import mutsa.hackathon.dto.MeUpdateRequest;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AppUserOnboardingIntegrationTest {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private Validator validator;

    @Test
    void 온보딩_정보를_저장하면_완료_상태가_된다() {
        AppUser user = saveUser();

        MeResponse before =
                appUserService.findMe(
                        user.getId()
                );

        assertFalse(
                before.onboardingCompleted()
        );

        assertNull(
                before.reminderTime()
        );

        assertFalse(
                before.aiMemoryConsent()
        );

        assertEquals(
                LocalTime.MIDNIGHT,
                before.dayStartTime()
        );

        MeResponse response =
                appUserService.updateMe(
                        user.getId(),
                        new MeUpdateRequest(
                                "  데이빗  ",
                                LocalTime.of(21, 0),
                                LocalTime.of(6, 0),
                                true
                        )
                );

        assertEquals(
                "데이빗",
                response.nickname()
        );

        assertEquals(
                LocalTime.of(21, 0),
                response.reminderTime()
        );

        assertEquals(
                LocalTime.of(6, 0),
                response.dayStartTime()
        );

        assertTrue(
                response.aiMemoryConsent()
        );

        assertTrue(
                response.onboardingCompleted()
        );

        assertTrue(
                response.onboardingCompletedAt()
                        != null
        );
    }

    @Test
    void 최초_온보딩에서는_AI_기억_활용_동의가_필수다() {
        AppUser user = saveUser();

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                appUserService.updateMe(
                                        user.getId(),
                                        new MeUpdateRequest(
                                                "데이빗",
                                                LocalTime.of(
                                                        20,
                                                        0
                                                ),
                                                LocalTime.of(6, 0),
                                                false
                                        )
                                )
                );

        assertEquals(
                ErrorCode
                        .ONBOARDING_CONSENT_REQUIRED,
                exception.getErrorCode()
        );
    }

    @Test
    void 온보딩_후_카카오로_재로그인해도_DAYBIT_닉네임을_유지한다() {
        AppUser user = saveUser();

        appUserService.updateMe(
                user.getId(),
                new MeUpdateRequest(
                        "데이빗",
                        LocalTime.of(22, 0),
                        LocalTime.of(6, 0),
                        true
                )
        );

        appUserService.saveOrUpdate(
                "kakao",
                user.getProviderId(),
                "변경된 카카오 닉네임",
                "updated@example.com",
                "https://example.com/profile.png"
        );

        MeResponse response =
                appUserService.findMe(
                        user.getId()
                );

        assertEquals(
                "데이빗",
                response.nickname()
        );

        assertEquals(
                "updated@example.com",
                response.email()
        );

        assertEquals(
                "https://example.com/profile.png",
                response.profileImage()
        );

        assertTrue(
                response.onboardingCompleted()
        );
    }

    @Test
    void 온보딩_완료_후에는_AI_기억_활용_동의를_철회할_수_있다() {
        AppUser user = saveUser();

        appUserService.updateMe(
                user.getId(),
                new MeUpdateRequest(
                        "데이빗",
                        LocalTime.of(23, 0),
                        LocalTime.of(6, 0),
                        true
                )
        );

        AppUser savedUser =
                appUserRepository
                        .findById(user.getId())
                        .orElseThrow();

        savedUser.updateAiMemoryProfile(
                "기억할 사용자 정보"
        );

        MeResponse response =
                appUserService.updateMe(
                        user.getId(),
                        new MeUpdateRequest(
                                "데이빗",
                                LocalTime.of(23, 0),
                                LocalTime.of(6, 0),
                                false
                        )
                );

        assertFalse(
                response.aiMemoryConsent()
        );

        assertTrue(
                response.onboardingCompleted()
        );

        assertNull(
                savedUser.getAiMemoryProfile()
        );
    }

    @Test
    void 하루_전환_시간은_필수다() {
        Set<ConstraintViolation<MeUpdateRequest>> violations =
                validator.validate(
                        new MeUpdateRequest(
                                "데이빗",
                                LocalTime.of(22, 0),
                                null,
                                true
                        )
                );

        assertFalse(violations.isEmpty());
    }

    @Test
    void 닉네임은_2자에서_8자_사이여야_한다() {
        Set<ConstraintViolation<MeUpdateRequest>>
                shortViolations =
                validator.validate(
                        new MeUpdateRequest(
                                "한",
                                LocalTime.of(20, 0),
                                LocalTime.of(6, 0),
                                true
                        )
                );

        Set<ConstraintViolation<MeUpdateRequest>>
                longViolations =
                validator.validate(
                        new MeUpdateRequest(
                                "아주긴닉네임입니다",
                                LocalTime.of(20, 0),
                                LocalTime.of(6, 0),
                                true
                        )
                );

        assertFalse(
                shortViolations.isEmpty()
        );

        assertFalse(
                longViolations.isEmpty()
        );
    }

    private AppUser saveUser() {
        return appUserRepository.save(
                AppUser.createKakaoUser(
                        "test-provider-"
                                + System.nanoTime(),
                        "카카오닉네임",
                        null,
                        "https://example.com/kakao.png"
                )
        );
    }
}
