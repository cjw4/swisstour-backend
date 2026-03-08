package dg.swiss.swiss_dg_db.security;

import dg.swiss.swiss_dg_db.security.jwt.JwtUtil;
import dg.swiss.swiss_dg_db.user.CustomUser;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Log In")
public class LoginController {

    private final AuthenticationManager authenticationManager;

    public LoginController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CustomUser user) {
        try {
            // Log authentication attempt
            System.out.println("Login attempt for user:" + user.getUsername());

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // Log successful authentication
            System.out.println("Authentication successful for user: " + user.getUsername());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwtToken = JwtUtil.generateToken((User) authentication.getPrincipal());
            return ResponseEntity.ok(jwtToken);

        } catch (BadCredentialsException e) {
            // Specific handling for invalid credentials
            System.out.println("Invalid credentials for user: " + user.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");

        } catch (DisabledException e) {
            // Handling for disabled user account
            System.out.println("Account disabled for user: " + user.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is disabled");

        } catch (LockedException e) {
            // Handling for locked user account
            System.out.println("Account locked for user: " + user.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is locked");

        } catch (Exception e) {
            // Generic error handling for any other authentication failures
            System.out.println("Authentication error for user: " + user.getUsername() + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Authentication failed: " + e.getMessage());
        }
    }
}
