package es.uni.reservas.controller;

import es.uni.reservas.dto.AuthDto;
import es.uni.reservas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthDto.LoginRespuesta login(@Valid @RequestBody AuthDto.LoginSolicitud solicitud) {
        return authService.login(solicitud);
    }
}
