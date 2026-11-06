package team.projectpulse.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * FR-SEC-active-account at the unit level: the token of an active account passes, the token of a deactivated
 * account, of an account that no longer exists, or with no {@code userId} claim fails.
 */
@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtValidatorTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ActiveAccountJwtValidator validator;

    private static Jwt tokenFor(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).subject("someone");
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.build();
    }

    @Test
    @DisplayName("An active account's token passes")
    void activeAccountPasses() {
        given(this.userRepository.findById(4)).willReturn(Optional.of(new Student("j", "John", "Smith", "j@abc.edu", "x", true, "student")));

        OAuth2TokenValidatorResult result = this.validator.validate(tokenFor(4L));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("A deactivated account's token fails")
    void deactivatedAccountFails() {
        given(this.userRepository.findById(4)).willReturn(Optional.of(new Student("j", "John", "Smith", "j@abc.edu", "x", false, "student")));

        OAuth2TokenValidatorResult result = this.validator.validate(tokenFor(4L));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).extracting("description").containsExactly(ActiveAccountJwtValidator.DEACTIVATED_DESCRIPTION);
    }

    @Test
    @DisplayName("A token naming no account, or with no userId claim, fails closed")
    void unknownOrMissingUserIdFails() {
        given(this.userRepository.findById(999)).willReturn(Optional.empty());

        assertThat(this.validator.validate(tokenFor(999L)).hasErrors()).isTrue();
        assertThat(this.validator.validate(tokenFor(null)).hasErrors()).isTrue();
    }

}
