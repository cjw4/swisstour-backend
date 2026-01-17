package dg.swiss.swiss_dg_db.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@Tag(name = "Create User")
public class CreateNewUserController {

    private final PasswordEncoder passwordEncoder;
    private final CustomUserRepository customUserRepository;

    public CreateNewUserController(PasswordEncoder passwordEncoder,
                                   CustomUserRepository customUserRepository) {
        this.passwordEncoder = passwordEncoder;
        this.customUserRepository = customUserRepository;
    }

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestBody CustomUser user) {
        Optional<CustomUser> optionalUser = customUserRepository.findById(user.getUsername());
        if (optionalUser.isEmpty()) {
            customUserRepository.save(new CustomUser(user.getUsername(), passwordEncoder.encode(user.getPassword())));
            return ResponseEntity.ok("User created");
        }
        return ResponseEntity.badRequest().body("Username already exists");
    }

}
