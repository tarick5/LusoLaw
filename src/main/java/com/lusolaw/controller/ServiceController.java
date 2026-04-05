package com.lusolaw.controller;

import com.lusolaw.dto.ApiMessageResponse;
import com.lusolaw.dto.ServiceCreateRequest;
import com.lusolaw.dto.ServiceResponse;
import com.lusolaw.mapper.ApiMapper;
import com.lusolaw.model.Service;
import com.lusolaw.model.User;
import com.lusolaw.repository.ServiceRepository;
import com.lusolaw.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;

    public ServiceController(ServiceRepository serviceRepository, CurrentUserService currentUserService) {
        this.serviceRepository = serviceRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ServiceResponse> listAll() {
        return serviceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ApiMapper::toService)
                .toList();
    }

    @GetMapping("/lawyer/{lawyerId}")
    public List<ServiceResponse> listByLawyer(@PathVariable Long lawyerId) {
        return serviceRepository.findByLawyerIdOrderByCreatedAtDesc(lawyerId).stream()
                .map(ApiMapper::toService)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceCreateRequest request) {
        User lawyer = currentUserService.requireRole(User.Role.LAWYER);

        Service service = new Service();
        service.setName(request.name().trim());
        service.setDescription(request.description().trim());
        service.setPrice(request.price());
        service.setLawyer(lawyer);

        Service saved = serviceRepository.save(service);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiMapper.toService(saved));
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceCreateRequest request
    ) {
        User lawyer = currentUserService.requireRole(User.Role.LAWYER);

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Servico nao encontrado"));

        if (!service.getLawyer().getId().equals(lawyer.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Nao pode editar servicos de outro advogado");
        }

        service.setName(request.name().trim());
        service.setDescription(request.description().trim());
        service.setPrice(request.price());

        Service saved = serviceRepository.save(service);
        return ResponseEntity.ok(ApiMapper.toService(saved));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<ApiMessageResponse> deleteService(@PathVariable Long serviceId) {
        User lawyer = currentUserService.requireRole(User.Role.LAWYER);

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Servico nao encontrado"));

        if (!service.getLawyer().getId().equals(lawyer.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Nao pode remover servicos de outro advogado");
        }

        serviceRepository.delete(service);
        return ResponseEntity.ok(new ApiMessageResponse("Servico removido com sucesso"));
    }
}
