package com.paralert.service;

import com.paralert.dto.request.UpdateProfileRequest;
import com.paralert.dto.request.VerifyDniRequest;
import com.paralert.dto.response.UserProfileResponse;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoUsuario;
import com.paralert.repository.UsuarioRepository;
import com.paralert.util.NivelConfianzaUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DniService dniService;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NivelConfianzaUtil nivelConfianzaUtil;

    @InjectMocks
    private PerfilService perfilService;

    @Test
    public void testActualizarPerfil_UsuarioNoVerificado() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombres("Jose");
        usuario.setApellidos("Elias");
        usuario.setVerificado(false);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRoles(new HashSet<>());

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNombres("Jose Manuel");
        req.setApellidos("Elias Saravia");
        req.setTelefono("987654321");

        UserProfileResponse res = perfilService.actualizarPerfil(usuario, req);

        assertNotNull(res);
        assertEquals("Jose Manuel", res.getNombres());
        assertEquals("Elias Saravia", res.getApellidos());
        assertEquals("987654321", res.getTelefono());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    public void testVerificarDni_Exito() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombres("Jose");
        usuario.setApellidos("Elias");
        usuario.setVerificado(false);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRoles(new HashSet<>());

        VerifyDniRequest req = new VerifyDniRequest();
        req.setDni("76543210");

        DniService.DniData mockData = new DniService.DniData("Jose Manuel", "Elias Saravia");

        when(usuarioRepository.existsByDni("76543210")).thenReturn(false);
        when(dniService.consultarDni("76543210")).thenReturn(mockData);
        when(nivelConfianzaUtil.getNivelVerificado()).thenReturn(80);

        UserProfileResponse res = perfilService.verificarDni(usuario, req);

        assertNotNull(res);
        assertTrue(res.getVerificado());
        assertEquals("Jose Manuel", res.getNombres());
        assertEquals("Elias Saravia", res.getApellidos());
        assertEquals(80, res.getNivelConfianza());
        verify(usuarioRepository, times(1)).save(usuario);
    }
}
