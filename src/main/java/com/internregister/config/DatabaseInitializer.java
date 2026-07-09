package com.internregister.config;

import com.internregister.entity.User;
import com.internregister.entity.Department;
import com.internregister.entity.Supervisor;
import com.internregister.entity.Admin;
import com.internregister.repository.UserRepository;
import com.internregister.repository.DepartmentRepository;
import com.internregister.repository.SupervisorRepository;
import com.internregister.repository.AdminRepository;
import com.internregister.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final DepartmentRepository departmentRepository;
    private final SupervisorRepository supervisorRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DatabaseInitializer(
            UserRepository userRepository, 
            @Lazy UserService userService,
            DepartmentRepository departmentRepository,
            SupervisorRepository supervisorRepository,
            AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.departmentRepository = departmentRepository;
        this.supervisorRepository = supervisorRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("\n=== Starting Database Initialization ===");

        // 1. Ensure Default Department exists
        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department newDept = new Department();
            newDept.setName("ICT Department");
            Department savedDept = departmentRepository.save(newDept);
            System.out.println("✓ Created default Department: " + savedDept.getName());
            return savedDept;
        });

        // 2. Ensure Users exist
        ensureUserExists("superadmin@univen.ac.za", "admin123", User.Role.SUPER_ADMIN);
        ensureUserExists("admin@univen.ac.za", "admin123", User.Role.ADMIN);
        ensureUserExists("supervisor@univen.ac.za", "supervisor123", User.Role.SUPERVISOR);
        ensureUserExists("intern@univen.ac.za", "intern123", User.Role.INTERN);

        // 3. Ensure Default Supervisor entity exists
        java.util.Optional<Supervisor> existingSup = supervisorRepository.findByEmail("supervisor@univen.ac.za").stream().findFirst();
        if (existingSup.isEmpty()) {
            Supervisor supervisor = new Supervisor();
            supervisor.setName("Test Supervisor");
            supervisor.setEmail("supervisor@univen.ac.za");
            supervisor.setStaffNumber("SUP8820");
            supervisor.setDepartment(department);
            supervisor.setField("Software Development");
            supervisorRepository.save(supervisor);
            System.out.println("✓ Created default Supervisor entity: Test Supervisor (Email: supervisor@univen.ac.za, Field: Software Development)");
        } else {
            Supervisor supervisor = existingSup.get();
            supervisor.setField("Software Development");
            supervisorRepository.save(supervisor);
            System.out.println("✓ Updated default Supervisor entity field to: Software Development");
        }

        // 4. Ensure Default Admin entity exists
        if (adminRepository.findByEmail("admin@univen.ac.za").stream().findFirst().isEmpty()) {
            Admin admin = new Admin();
            admin.setName("Test Admin");
            admin.setEmail("admin@univen.ac.za");
            admin.setStaffNumber("ADM8821");
            admin.setDepartment(department);
            admin.setActive(true);
            adminRepository.save(admin);
            System.out.println("✓ Created default Admin entity: Test Admin (Email: admin@univen.ac.za)");
        }

        System.out.println("=== Database Initialization Complete ===\n");
    }

    private void ensureUserExists(String email, String password, User.Role role) {
        if (userRepository.findByUsername(email).isEmpty()) {
            User user = new User();
            user.setUsername(email);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setActive(true);
            User saved = userService.saveUser(user);
            System.out.println("✓ Created " + role + " user: " + email + " (ID: " + saved.getId() + ")");
        } else {
            System.out.println("- User already exists: " + email + " (" + role + ")");
        }
    }
}

