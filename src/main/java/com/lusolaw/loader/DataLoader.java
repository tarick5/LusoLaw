package com.lusolaw.loader;

import com.lusolaw.model.Service;
import com.lusolaw.model.User;
import com.lusolaw.repository.ServiceRepository;
import com.lusolaw.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    public DataLoader(UserRepository userRepository, ServiceRepository serviceRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        User lawyer1 = new User();
        lawyer1.setName("Marta Ribeiro");
        lawyer1.setEmail("marta@lusolaw.pt");
        lawyer1.setPassword(passwordEncoder.encode("password123"));
        lawyer1.setRole(User.Role.LAWYER);
        lawyer1.setPhone("+351 912 345 678");
        lawyer1.setAddress("Lisboa");
        lawyer1.setSpecialization("Imigracao e visto de residencia");
        lawyer1.setLawyerRegistrationNumber("OA-PT-12001");
        lawyer1.setIdentificationNumber("CC-10001234");
        lawyer1.setAccountStatus(User.AccountStatus.ACTIVE);
        lawyer1.setPricePerHour(new BigDecimal("120.00"));
        userRepository.save(lawyer1);

        User lawyer2 = new User();
        lawyer2.setName("Joao Ferreira");
        lawyer2.setEmail("joao@lusolaw.pt");
        lawyer2.setPassword(passwordEncoder.encode("password123"));
        lawyer2.setRole(User.Role.LAWYER);
        lawyer2.setPhone("+351 913 987 654");
        lawyer2.setAddress("Porto");
        lawyer2.setSpecialization("Reagrupamento familiar e AR");
        lawyer2.setLawyerRegistrationNumber("OA-PT-12002");
        lawyer2.setIdentificationNumber("CC-10004567");
        lawyer2.setAccountStatus(User.AccountStatus.ACTIVE);
        lawyer2.setPricePerHour(new BigDecimal("110.00"));
        userRepository.save(lawyer2);

        Service service1 = new Service();
        service1.setName("Titulo de residencia");
        service1.setDescription("Apoio completo para pedido de titulo de residencia e acompanhamento do processo.");
        service1.setPrice(new BigDecimal("950.00"));
        service1.setLawyer(lawyer1);
        serviceRepository.save(service1);

        Service service2 = new Service();
        service2.setName("Reagrupamento familiar");
        service2.setDescription("Planeamento e documentacao para reagrupamento de familia em Portugal.");
        service2.setPrice(new BigDecimal("1200.00"));
        service2.setLawyer(lawyer2);
        serviceRepository.save(service2);

        Service service3 = new Service();
        service3.setName("Renovacao de AR");
        service3.setDescription("Renovacao de autorizacao de residencia de forma segura e sem erros.");
        service3.setPrice(new BigDecimal("650.00"));
        service3.setLawyer(lawyer1);
        serviceRepository.save(service3);
    }
}
