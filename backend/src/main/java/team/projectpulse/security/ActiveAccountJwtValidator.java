package team.projectpulse.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a token whose account has been deactivated since the token was issued (FR-SEC-active-account).
 *
 * <p>A JWT stays valid until it expires, so checking {@code enabled} only at login would let a student or
 * instructor who is deactivated while logged in keep access for the rest of the token's lifetime. This
 * validator runs inside the {@code JwtDecoder} on every authenticated request, after the signature and expiry
 * checks, and looks the account up by the token's {@code userId} claim. A failure surfaces as
 * {@code InvalidBearerTokenException}, which the bearer-token entry point already answers with {@code 401}.
 *
 * <p>It fails closed: a token with no usable {@code userId}, or one naming no account, is refused too.
 */
@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    static final String DEACTIVATED_DESCRIPTION = "The account is deactivated.";

    private final UserRepository userRepository;


    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Integer userId = readUserId(jwt);
        boolean active = userId != null && this.userRepository.findById(userId)
                .map(PeerEvaluationUser::isEnabled)
                .orElse(false);
        if (active) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, DEACTIVATED_DESCRIPTION, null));
    }

    private static Integer readUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Number number) {
            return number.intValue();
        }
        if (claim instanceof String string) {
            try {
                return Integer.valueOf(string);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
