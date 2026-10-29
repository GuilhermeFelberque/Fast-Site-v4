package com.example.demo.service;

import com.example.demo.dto.VeiculoRequest;
import com.example.demo.model.Veiculo;
import com.example.demo.repository.CadastroRepository;
import com.example.demo.repository.VeiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * SERVICE: regras de negócio dos veículos.
 */
@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final CadastroRepository cadastroRepository;

    public VeiculoService(VeiculoRepository veiculoRepository, CadastroRepository cadastroRepository) {
        this.veiculoRepository = veiculoRepository;
        this.cadastroRepository = cadastroRepository;
    }

    public List<Veiculo> listar() {
        return veiculoRepository.findAll();
    }

    public Veiculo buscar(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo #" + id + " não encontrado"));
    }

    public Veiculo criar(VeiculoRequest dados) {
        Veiculo v = validarEMontar(dados, null);
        Long id = veiculoRepository.insert(v);
        return buscar(id);
    }

    public Veiculo atualizar(Long id, VeiculoRequest dados) {
        buscar(id); // 404 se não existir
        Veiculo v = validarEMontar(dados, id);
        veiculoRepository.update(id, v);
        return buscar(id);
    }

    public void excluir(Long id) {
        buscar(id);
        int ordens = veiculoRepository.contarOrdens(id);
        if (ordens > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este veículo tem " + ordens + " ordem(ns) de serviço. Exclua as ordens antes.");
        }
        veiculoRepository.delete(id);
    }

    private Veiculo validarEMontar(VeiculoRequest dados, Long idAtual) {
        // Placa sempre em maiúsculas e no formato ABC-1234 (antiga) ou ABC1D23 (Mercosul)
        String placa = dados.placa().trim().toUpperCase().replace("-", "");
        if (Character.isDigit(placa.charAt(4))) {
            placa = placa.substring(0, 3) + "-" + placa.substring(3);
        }

        if (veiculoRepository.existsPlaca(placa, idAtual)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um veículo com a placa " + placa);
        }
        if (cadastroRepository.findAtivoById(dados.clienteId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente não encontrado");
        }

        Veiculo v = new Veiculo();
        v.setPlaca(placa);
        v.setModelo(dados.modelo().trim());
        v.setAno(dados.ano());
        v.setClienteId(dados.clienteId());
        return v;
    }
}
