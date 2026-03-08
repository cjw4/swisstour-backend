package dg.swiss.swiss_dg_db.player;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import org.springframework.web.servlet.HandlerMapping;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/** Validate that the sdaNumber value isn't taken yet. */
@Target({FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PlayerSdaNumberUnique.PlayerSdaNumberUniqueValidator.class)
public @interface PlayerSdaNumberUnique {

    String message() default "{Exists.player.sdaNumber}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PlayerSdaNumberUniqueValidator
            implements ConstraintValidator<PlayerSdaNumberUnique, Long> {

        private final PlayerService playerService;
        private final HttpServletRequest request;

        public PlayerSdaNumberUniqueValidator(
                final PlayerService playerService, final HttpServletRequest request) {
            this.playerService = playerService;
            this.request = request;
        }

        @Override
        public boolean isValid(final Long value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }
            @SuppressWarnings("unchecked")
            final Map<String, String> pathVariables =
                    ((Map<String, String>)
                            request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
            final String currentId = pathVariables.get("id");
            if (currentId != null
                    && value.equals(playerService.get(Long.parseLong(currentId)).getSdaNumber())) {
                // value hasn't changed
                return true;
            }
            return !playerService.sdaNumberExists(value);
        }
    }
}
