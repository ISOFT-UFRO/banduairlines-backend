package com.banduairlines.service.cuenta;

import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.BoletoRepository;
import com.banduairlines.security.UsuarioActual;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class VuelosCuentaService {
    private final BoletoRepository boletos;
    private final UsuarioActual usuarioActual;

    public VuelosCuentaService(BoletoRepository boletos, UsuarioActual usuarioActual) {
        this.boletos = boletos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Long> consultarActual() {
        var usuario = usuarioActual.exigir();
        if (usuario.rol() != RolUsuario.PASAJERO) {
            throw new AccessDeniedException("Solo pasajeros pueden consultar sus vuelos");
        }
        return boletos.buscarIdsVuelosConfirmados(usuario.id());
    }
}
