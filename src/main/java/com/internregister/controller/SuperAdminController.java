package com.internregister.controller;

import com.internregister.entity.Admin;
import com.internregister.entity.User;
import com.internregister.entity.Department;
import com.internregister.entity.Supervisor;
import com.internregister.repository.AdminRepository;
import com.internregister.repository.UserRepository;
import com.internregister.repository.DepartmentRepository;
import com.internregister.repository.SupervisorRepository;
import com.internregister.service.WebSocketService;
import com.internregister.service.ActivityLogService;
import com.internregister.util.SecurityUtil;
import com.internregister.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/super-admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SupervisorRepository supervisorRepository;
    private final SecurityUtil securityUtil;
    private final WebSocketService webSocketService;
    private final ActivityLogService activityLogService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SuperAdminController(
            AdminRepository adminRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            SupervisorRepository supervisorRepository,
            SecurityUtil securityUtil,
            WebSocketService webSocketService,
            ActivityLogService activityLogService,
            EmailService emailService) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.supervisorRepository = supervisorRepository;
        this.securityUtil = securityUtil;
        this.webSocketService = webSocketService;
        this.activityLogService = activityLogService;
        this.emailService = emailService;
    }

    /**
     * Get all admins (Super Admin only)
     */
    @GetMapping("/admins")
    public ResponseEntity<?> getAllAdmins() {
        try {
            securityUtil.requireSuperAdmin();

            List<Admin> admins = adminRepository.findAll(); // This now eagerly loads departments via @EntityGraph
            List<Map<String, Object>> adminList = admins.stream().map(admin -> {
                Map<String, Object> adminMap = new HashMap<>();
                adminMap.put("adminId", admin.getAdminId());
                adminMap.put("name", admin.getName());
                adminMap.put("email", admin.getEmail());
                adminMap.put("createdAt", admin.getCreatedAt() != null ? admin.getCreatedAt().toString() : null);

                // Check if user exists and get userId
                Optional<User> userOpt = userRepository.findByUsername(admin.getEmail()).stream().findFirst();
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    adminMap.put("userId", user.getId());
                    adminMap.put("active", user.getActive() != null ? user.getActive() : true);
                    adminMap.put("hasSignature", user.getSignature() != null && !user.getSignature().trim().isEmpty());
                    String lastLoginAt = user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null;
                    adminMap.put("lastLoginAt", lastLoginAt);
                    // Debug logging
                    System.out.println(
                            "✓ Admin " + admin.getName() + " (" + admin.getEmail() + ") - lastLoginAt: " + lastLoginAt);
                } else {
                    adminMap.put("userId", null);
                    adminMap.put("active", false);
                    adminMap.put("hasSignature", false);
                    adminMap.put("lastLoginAt", null);
                    System.out.println("⚠ Admin " + admin.getName() + " (" + admin.getEmail()
                            + ") - User not found, lastLoginAt: null");
                }

                // Get department if linked - department is now eagerly loaded
                if (admin.getDepartment() != null) {
                    adminMap.put("departmentId", admin.getDepartment().getDepartmentId());
                    adminMap.put("departmentName", admin.getDepartment().getName());
                    System.out.println(
                            "✓ Admin " + admin.getName() + " has department: " + admin.getDepartment().getName()
                                    + " (ID: " + admin.getDepartment().getDepartmentId() + ")");
                } else {
                    adminMap.put("departmentId", null);
                    adminMap.put("departmentName", null);
                    System.out.println("⚠ Admin " + admin.getName() + " has no department assigned");
                }

                return adminMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(adminList);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve admins: " + e.getMessage()));
        }
    }

    /**
     * Send invitation email to admin (Super Admin only)
     */
    @PostMapping("/admins/send-invite")
    public ResponseEntity<?> sendAdminInvite(@RequestBody Map<String, String> body) {
        try {
            securityUtil.requireSuperAdmin();

            String email = body.get("email");
            String name = body.getOrDefault("name", "");
            String sendToEmail = body.get("sendToEmail");
            String inviteLink = body.get("inviteLink");
            String message = body.get("message");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            String trimmedEmail = email.trim().toLowerCase();

            // Validate email domain
            if (!trimmedEmail.endsWith("@univen.ac.za")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "Invalid email domain",
                        "message", "Only University of Venda (@univen.ac.za) email addresses are allowed."));
            }

            // Generate a temporary password for the invite
            String tempPassword = UUID.randomUUID().toString().substring(0, 8) + "1!Aa";

            String recipientEmail = (sendToEmail != null && !sendToEmail.trim().isEmpty()) ? sendToEmail.trim() : trimmedEmail;

            // Send actual invitation email via EmailService
            if (message != null && !message.trim().isEmpty() && inviteLink != null) {
                emailService.sendAdminInvite(recipientEmail, name, tempPassword, inviteLink, message);
            } else {
                emailService.sendAdminInvite(recipientEmail, name, tempPassword);
            }

            System.out.println("===========================================");
            System.out.println("ADMIN INVITATION EMAIL SENT");
            System.out.println("To: " + recipientEmail);
            System.out.println("Original Email: " + trimmedEmail);
            System.out.println("Name: " + name);
            System.out.println("Temp Password: " + tempPassword);
            if (inviteLink != null) {
                System.out.println("Invite Link: " + inviteLink);
            }
            if (message != null) {
                System.out.println("Message: " + message);
            }
            System.out.println("===========================================");

            return ResponseEntity.ok(Map.of(
                    "message", "Invitation email sent successfully",
                    "email", recipientEmail,
                    "tempPassword", tempPassword));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send invitation: " + e.getMessage()));
        }
    }

    /**
     * Create a new admin (Super Admin only)
     */
    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, Object> body) {
        try {
            securityUtil.requireSuperAdmin();

            String name = body.get("name") != null ? body.get("name").toString() : null;
            String surname = body.get("surname") != null ? body.get("surname").toString() : null;
            String email = body.get("email") != null ? body.get("email").toString() : null;
            String staffNumber = body.get("staffNumber") != null ? body.get("staffNumber").toString() : null;
            String signature = body.get("signature") != null ? body.get("signature").toString() : null;
            Object departmentIdObj = body.get("departmentId");

            // Debug logging
            System.out.println("DEBUG createAdmin - departmentIdObj: " + departmentIdObj +
                    " (type: " + (departmentIdObj != null ? departmentIdObj.getClass().getName() : "null") + ")");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            if (staffNumber == null || staffNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Staff Number is required"));
            }

            // For Univen API accounts, we'll set a default local password of "admin123"
            // as a fallback in case the Univen API is offline or times out.
            String defaultPassword = "admin123";

            String trimmedEmail = email.trim().toLowerCase();

            // Validate email domain
            if (!trimmedEmail.endsWith("@univen.ac.za")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "Invalid email domain",
                        "message", "Only University of Venda (@univen.ac.za) email addresses are allowed."));
            }

            // Check if user already exists (by email or staff number)
            if (userRepository.findByUsername(trimmedEmail).stream().findFirst().isPresent() ||
                    adminRepository.findByEmail(trimmedEmail).stream().findFirst().isPresent() ||
                    adminRepository.findByStaffNumber(staffNumber.trim()).stream().findFirst().isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Admin with this email or staff number already exists"));
            }

            // Create User
            User user = new User();
            user.setUsername(trimmedEmail);
            user.setEmail(trimmedEmail);
            user.setStaffNumber(staffNumber.trim());
            user.setPassword(passwordEncoder.encode(defaultPassword));
            user.setRole(User.Role.ADMIN);
            user.setRequiresPasswordChange(false); // No local password change required
            User savedUser = userRepository.save(user);

            // Create Admin profile
            Admin admin = new Admin();
            // Combine name and surname for retrieval
            String fullName = (surname != null && !surname.trim().isEmpty())
                    ? name.trim() + " " + surname.trim()
                    : name.trim();
            admin.setName(fullName);
            admin.setEmail(trimmedEmail);
            admin.setStaffNumber(staffNumber.trim());

            // Set department if provided - handle different types (Number, String, null)
            Long departmentId = null;
            if (departmentIdObj != null) {
                if (departmentIdObj instanceof Number) {
                    departmentId = ((Number) departmentIdObj).longValue();
                } else if (departmentIdObj instanceof String) {
                    String departmentIdStr = ((String) departmentIdObj).trim();
                    // Handle empty string, "null", "undefined" as null
                    if (!departmentIdStr.isEmpty() &&
                            !"null".equalsIgnoreCase(departmentIdStr) &&
                            !"undefined".equalsIgnoreCase(departmentIdStr)) {
                        try {
                            departmentId = Long.parseLong(departmentIdStr);
                        } catch (NumberFormatException e) {
                            System.out.println("WARNING: Invalid department ID format: " + departmentIdStr);
                            // Continue without department
                        }
                    }
                }
            }

            if (departmentId != null) {
                Optional<Department> deptOpt = departmentRepository.findById(departmentId);
                if (deptOpt.isPresent()) {
                    admin.setDepartment(deptOpt.get());
                    System.out.println("✓ Department set: " + deptOpt.get().getName() + " (ID: " + departmentId + ")");
                } else {
                    System.out.println("WARNING: Department not found with ID: " + departmentId);
                }
            } else {
                System.out.println("INFO: No department provided for admin");
            }

            Admin savedAdmin = adminRepository.save(admin);

            // ✅ Auto-create default supervisor if department is set and has no supervisor
            if (savedAdmin.getDepartment() != null) {
                Department department = savedAdmin.getDepartment();
                List<Supervisor> deptSups = supervisorRepository.findByDepartmentId(department.getDepartmentId());
                if (deptSups.isEmpty()) {
                    String deptCleanName = department.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                    String supervisorEmail = "supervisor_" + deptCleanName + "@univen.ac.za";

                    // Default supervisor email for standard ICT department
                    if ("ictdepartment".equals(deptCleanName)) {
                        supervisorEmail = "supervisor@univen.ac.za";
                    }

                    if (userRepository.findByUsername(supervisorEmail).isEmpty()) {
                        // Create User account
                        User supUser = new User();
                        supUser.setUsername(supervisorEmail);
                        supUser.setEmail(supervisorEmail);
                        supUser.setPassword(passwordEncoder.encode("supervisor123"));
                        supUser.setRole(User.Role.SUPERVISOR);
                        supUser.setActive(true);
                        userRepository.save(supUser);

                        // Create Supervisor profile
                        Supervisor supervisor = new Supervisor();
                        supervisor.setName("Default Supervisor - " + department.getName());
                        supervisor.setEmail(supervisorEmail);
                        supervisor.setStaffNumber("SUP-" + department.getDepartmentId() + "-DFT");
                        supervisor.setDepartment(department);
                        supervisor.setField("Software Development");
                        supervisorRepository.save(supervisor);
                        System.out.println("✓ Auto-created default Supervisor user/profile for department: " 
                            + department.getName() + " (Email: " + supervisorEmail + ", Password: supervisor123)");
                    }
                }
            }

            // Log admin creation
            securityUtil.getCurrentUser()
                    .ifPresent(currentUser -> activityLogService.log(currentUser.getUsername(), "CREATE_ADMIN",
                            "Created new admin: " + email + " in department: " +
                                    (admin.getDepartment() != null ? admin.getDepartment().getName() : "None"),
                            null));

            // Fetch final object to return
            // Reload admin to ensure department is properly loaded
            Long adminId = savedAdmin.getAdminId();
            if (adminId != null) {
                savedAdmin = adminRepository.findById(adminId).orElse(savedAdmin);
            }

            // Set signature if provided
            if (signature != null && !signature.trim().isEmpty()) {
                savedUser.setSignature(signature.trim());
                userRepository.save(savedUser);
            }

            System.out.println("✓ Admin created successfully:");
            System.out.println("  Admin ID: " + savedAdmin.getAdminId());
            System.out.println("  User ID: " + savedUser.getId());
            System.out.println("  Name: " + savedAdmin.getName());
            System.out.println("  Email: " + savedAdmin.getEmail());
            System.out.println("  Staff Number: " + savedAdmin.getStaffNumber());
            if (savedAdmin.getDepartment() != null) {
                System.out.println("  Department: " + savedAdmin.getDepartment().getName() + " (ID: "
                        + savedAdmin.getDepartment().getDepartmentId() + ")");
            } else {
                System.out.println("  Department: null (not assigned)");
            }

            // Since we are using Univen Staff API, we don't send a local password.
            // We just send a welcome email informing them they can log in with their
            // credentials.
            emailService.sendAdminWelcome(trimmedEmail, name.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin created successfully");
            response.put("adminId", savedAdmin.getAdminId());
            response.put("userId", savedUser.getId());
            response.put("email", savedAdmin.getEmail());
            response.put("name", savedAdmin.getName());
            response.put("staffNumber", savedAdmin.getStaffNumber());
            if (savedAdmin.getDepartment() != null) {
                response.put("departmentId", savedAdmin.getDepartment().getDepartmentId());
                response.put("departmentName", savedAdmin.getDepartment().getName());
            } else {
                response.put("departmentId", null);
                response.put("departmentName", null);
            }

            // Broadcast real-time update
            webSocketService.broadcastAdminUpdate("CREATED", response);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create admin: " + e.getMessage()));
        }
    }

    /**
     * Bulk import admins from CSV (Super Admin only)
     * Expected CSV headers: Name,Surname,Email,StaffNumber,Department
     */
    @PostMapping("/admins/bulk-csv")
    public ResponseEntity<?> bulkImportAdmins(@RequestBody Map<String, Object> body) {
        try {
            securityUtil.requireSuperAdmin();

            String csvContent = body.get("csvContent") != null ? body.get("csvContent").toString() : null;
            if (csvContent == null || csvContent.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "CSV content is required"));
            }

            List<Map<String, Object>> created = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();
            List<Map<String, Object>> failed = new ArrayList<>();

            String[] lines = csvContent.split("\n");
            if (lines.length < 2) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "CSV must have a header row and at least one data row"));
            }

            // Parse header row (case-insensitive)
            String[] headers = lines[0].trim().split(",");
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase().replaceAll("[^a-z]", ""), i);
            }

            for (int lineNum = 1; lineNum < lines.length; lineNum++) {
                String line = lines[lineNum].trim();
                if (line.isEmpty())
                    continue;

                String[] cols = line.split(",", -1);
                Map<String, Object> rowInfo = new HashMap<>();
                rowInfo.put("row", lineNum + 1);

                try {
                    String name = getValue(cols, headerIndex, "name", "firstname");
                    String surname = getValue(cols, headerIndex, "surname", "lastname");
                    String email = getValue(cols, headerIndex, "email");
                    String staffNumber = getValue(cols, headerIndex, "staffnumber", "staff");
                    String departmentName = getValue(cols, headerIndex, "department", "dept");

                    rowInfo.put("email", email != null ? email : "");
                    rowInfo.put("name", name != null ? name : "");

                    if (name == null || name.isEmpty() || email == null || email.isEmpty() || staffNumber == null
                            || staffNumber.isEmpty()) {
                        rowInfo.put("reason", "Missing required field(s): Name, Email, or StaffNumber");
                        failed.add(rowInfo);
                        continue;
                    }

                    String trimmedEmail = email.trim().toLowerCase();
                    if (!trimmedEmail.endsWith("@univen.ac.za")) {
                        rowInfo.put("reason", "Invalid email domain (must be @univen.ac.za)");
                        failed.add(rowInfo);
                        continue;
                    }

                    // Check duplicates
                    if (userRepository.findByUsername(trimmedEmail).stream().findFirst().isPresent() ||
                            adminRepository.findByEmail(trimmedEmail).stream().findFirst().isPresent() ||
                            adminRepository.findByStaffNumber(staffNumber.trim()).stream().findFirst().isPresent()) {
                        rowInfo.put("reason", "Admin with this email or staff number already exists");
                        skipped.add(rowInfo);
                        continue;
                    }

                    // Resolve department
                    Department department = null;
                    if (departmentName != null && !departmentName.trim().isEmpty()) {
                        department = departmentRepository.findByName(departmentName.trim()).orElse(null);
                        if (department == null) {
                            rowInfo.put("reason", "Department '" + departmentName.trim() + "' not found");
                            failed.add(rowInfo);
                            continue;
                        }
                    }

                    // Create User
                    User user = new User();
                    user.setUsername(trimmedEmail);
                    user.setEmail(trimmedEmail);
                    user.setStaffNumber(staffNumber.trim());
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setRole(User.Role.ADMIN);
                    user.setRequiresPasswordChange(false);
                    userRepository.save(user);

                    // Create Admin profile
                    Admin admin = new Admin();
                    String fullName = (surname != null && !surname.trim().isEmpty())
                            ? name.trim() + " " + surname.trim()
                            : name.trim();
                    admin.setName(fullName);
                    admin.setEmail(trimmedEmail);
                    admin.setStaffNumber(staffNumber.trim());
                    if (department != null)
                        admin.setDepartment(department);
                    adminRepository.save(admin);

                    // Send welcome email
                    try {
                        emailService.sendAdminWelcome(trimmedEmail, name.trim());
                    } catch (Exception emailEx) {
                        System.err.println(
                                "Failed to send welcome email to " + trimmedEmail + ": " + emailEx.getMessage());
                    }

                    rowInfo.put("department", department != null ? department.getName() : null);
                    created.add(rowInfo);

                } catch (Exception rowEx) {
                    rowInfo.put("reason", "Processing error: " + rowEx.getMessage());
                    failed.add(rowInfo);
                }
            }

            // Log bulk activity
            securityUtil.getCurrentUser()
                    .ifPresent(currentUser -> activityLogService.log(currentUser.getUsername(), "BULK_IMPORT_ADMINS",
                            "Bulk CSV import: " + created.size() + " created, " + skipped.size() + " skipped, "
                                    + failed.size() + " failed",
                            null));

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Bulk import completed");
            result.put("totalProcessed", lines.length - 1);
            result.put("created", created);
            result.put("skipped", skipped);
            result.put("failed", failed);
            result.put("createdCount", created.size());
            result.put("skippedCount", skipped.size());
            result.put("failedCount", failed.size());

            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Bulk import failed: " + e.getMessage()));
        }
    }

    private String getValue(String[] cols, Map<String, Integer> headerIndex, String... keys) {
        for (String key : keys) {
            Integer idx = headerIndex.get(key);
            if (idx != null && idx < cols.length) {
                String val = cols[idx].trim().replaceAll("^\"|\"$", ""); // strip quotes
                if (!val.isEmpty())
                    return val;
            }
        }
        return null;
    }

    /**
     * Update admin details (name, email, password, department) (Super Admin only)
     */
    @PutMapping("/admins/{adminId}")
    public ResponseEntity<?> updateAdmin(@PathVariable Long adminId, @RequestBody Map<String, Object> body) {
        try {
            securityUtil.requireSuperAdmin();

            if (adminId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin ID is required"));
            }
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();
            Optional<User> userOpt = userRepository.findByUsername(admin.getEmail()).stream().findFirst();
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found for this admin"));
            }

            User user = userOpt.get();
            String oldEmail = admin.getEmail();

            // Update name if provided
            if (body.containsKey("name")) {
                String name = (String) body.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    admin.setName(name.trim());
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "Name cannot be empty"));
                }
            }

            // Update email if provided
            if (body.containsKey("email")) {
                String email = (String) body.get("email");
                if (email != null && !email.trim().isEmpty()) {
                    String trimmedEmail = email.trim().toLowerCase();

                    // Validate email domain
                    if (!trimmedEmail.endsWith("@univen.ac.za")) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                                "error", "Invalid email domain",
                                "message", "Only University of Venda (@univen.ac.za) email addresses are allowed."));
                    }

                    // Check if new email already exists (and it's not the current admin's email)
                    if (!trimmedEmail.equals(oldEmail)) {
                        if (userRepository.findByUsername(trimmedEmail).stream().findFirst().isPresent() ||
                                adminRepository.findByEmail(trimmedEmail).stream().findFirst().isPresent()) {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(Map.of("error", "Email already exists"));
                        }
                        admin.setEmail(trimmedEmail);
                        user.setUsername(trimmedEmail);
                        user.setEmail(trimmedEmail);
                    }
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email cannot be empty"));
                }
            }

            // Update password if provided
            if (body.containsKey("password")) {
                String password = (String) body.get("password");
                if (password != null && !password.trim().isEmpty()) {
                    if (password.length() < 6) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Password must be at least 6 characters long"));
                    }
                    user.setPassword(passwordEncoder.encode(password.trim()));
                }
            }

            // Update department if provided
            // NOTE: Only update department if explicitly provided in the request
            // If not provided, preserve the existing department
            if (body.containsKey("departmentId")) {
                Object deptIdObj = body.get("departmentId");
                Long departmentId = null;
                if (deptIdObj != null) {
                    if (deptIdObj instanceof Number) {
                        departmentId = ((Number) deptIdObj).longValue();
                    } else if (deptIdObj instanceof String) {
                        String deptIdStr = ((String) deptIdObj).trim();
                        if (deptIdStr.isEmpty() || "null".equalsIgnoreCase(deptIdStr)
                                || "undefined".equalsIgnoreCase(deptIdStr)) {
                            departmentId = null;
                        } else {
                            try {
                                departmentId = Long.parseLong(deptIdStr);
                            } catch (NumberFormatException e) {
                                return ResponseEntity.badRequest().body(Map.of("error", "Invalid department ID"));
                            }
                        }
                    }
                }

                System.out.println("DEBUG updateAdmin - departmentId from request: " + departmentId +
                        " (orig object type: " + (deptIdObj != null ? deptIdObj.getClass().getName() : "null") + ")");

                if (departmentId != null) {
                    Optional<Department> deptOpt = departmentRepository.findById(departmentId);
                    if (deptOpt.isPresent()) {
                        admin.setDepartment(deptOpt.get());
                        System.out.println("✓ Successfully updated department to: " + deptOpt.get().getName() + " (ID: "
                                + departmentId + ")");
                    } else {
                        System.out.println("⚠ ERROR: Department not found with ID: " + departmentId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Department not found"));
                    }
                } else {
                    // Explicitly setting to null (user wants to remove department)
                    admin.setDepartment(null);
                    System.out.println("⚠ Success: Explicitly removed department (user set to null)");
                }
            } else {
                // Department not in request - preserve existing department
                System.out.println("INFO: departmentId not in request body - preserving existing department");
                if (admin.getDepartment() != null) {
                    System.out.println("  Preserving department: " + admin.getDepartment().getName() + " (ID: "
                            + admin.getDepartment().getDepartmentId() + ")");
                } else {
                    System.out.println("  Admin has no department to preserve");
                }
            }

            // Save changes
            Admin updatedAdmin = adminRepository.save(admin);
            User updatedUser = userRepository.save(user);

            // Log admin update
            securityUtil.getCurrentUser()
                    .ifPresent(currentUser -> activityLogService.log(currentUser.getUsername(), "UPDATE_ADMIN",
                            "Updated admin: " + admin.getEmail(), null));

            // Reload admin to ensure department is properly loaded
            Long finalAdminId = updatedAdmin.getAdminId();
            if (finalAdminId != null) {
                updatedAdmin = adminRepository.findById(finalAdminId).orElse(updatedAdmin);
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin updated successfully");
            response.put("adminId", updatedAdmin.getAdminId());
            response.put("userId", updatedUser.getId());
            response.put("name", updatedAdmin.getName());
            response.put("email", updatedAdmin.getEmail());
            if (updatedAdmin.getDepartment() != null) {
                response.put("departmentId", updatedAdmin.getDepartment().getDepartmentId());
                response.put("departmentName", updatedAdmin.getDepartment().getName());
                System.out.println("✓ Admin department updated: " + updatedAdmin.getDepartment().getName() + " (ID: "
                        + updatedAdmin.getDepartment().getDepartmentId() + ")");
            } else {
                response.put("departmentId", null);
                response.put("departmentName", null);
                System.out.println("⚠ Admin department removed (set to null)");
            }

            // Broadcast real-time update
            webSocketService.broadcastAdminUpdate("UPDATED", response);
            webSocketService.broadcastUserUpdate("PROFILE_UPDATED", Map.of(
                    "email", updatedAdmin.getEmail(),
                    "name", updatedAdmin.getName(),
                    "department", updatedAdmin.getDepartment() != null ? updatedAdmin.getDepartment().getName() : "",
                    "role", "ADMIN"));

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update admin: " + e.getMessage()));
        }
    }

    /**
     * Update admin signature (Super Admin only)
     */
    @PutMapping("/admins/{adminId}/signature")
    public ResponseEntity<?> updateAdminSignature(@PathVariable Long adminId, @RequestBody Map<String, String> body) {
        try {
            securityUtil.requireSuperAdmin();

            String signature = body.get("signature");
            if (signature == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Signature is required"));
            }

            if (adminId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin ID is required"));
            }
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();
            Optional<User> userOpt = userRepository.findByUsername(admin.getEmail()).stream().findFirst();
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setSignature(signature);
                userRepository.save(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found for this admin"));
            }

            // Log admin signature update
            securityUtil.getCurrentUser().ifPresent(
                    currentUser -> activityLogService.log(currentUser.getUsername(), "UPDATE_ADMIN_SIGNATURE",
                            "Updated signature for admin: " + admin.getEmail(), null));

            return ResponseEntity.ok(Map.of("message", "Signature updated successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update signature: " + e.getMessage()));
        }
    }

    /**
     * Update admin department (Super Admin only)
     */
    @PutMapping("/admins/{adminId}/department")
    public ResponseEntity<?> updateAdminDepartment(@PathVariable Long adminId, @RequestBody Map<String, Object> body) {
        try {
            securityUtil.requireSuperAdmin();

            if (adminId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin ID is required"));
            }
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Admin not found"));
            }

            Object deptIdObj = body.get("departmentId");
            System.out.println("DEBUG: Received departmentId object: " + deptIdObj + " (type: "
                    + (deptIdObj != null ? deptIdObj.getClass().getName() : "null") + ")");
            Long departmentId = null;
            if (deptIdObj != null) {
                if (deptIdObj instanceof Number) {
                    departmentId = ((Number) deptIdObj).longValue();
                } else if (deptIdObj instanceof String) {
                    String deptIdStr = ((String) deptIdObj).trim();
                    // Handle empty string, "null", "undefined" as null
                    if (deptIdStr.isEmpty() || "null".equalsIgnoreCase(deptIdStr)
                            || "undefined".equalsIgnoreCase(deptIdStr)) {
                        departmentId = null;
                    } else {
                        try {
                            departmentId = Long.parseLong(deptIdStr);
                        } catch (NumberFormatException e) {
                            return ResponseEntity.badRequest().body(Map.of(
                                    "error", "Invalid department ID",
                                    "message", "Department ID must be a valid number. Received: " + deptIdStr));
                        }
                    }
                } else {
                    // If it's neither Number nor String, treat as null (to remove department)
                    departmentId = null;
                }
            }

            // Update department
            Admin admin = adminOpt.get();
            if (departmentId != null) {
                Optional<Department> deptOpt = departmentRepository.findById(departmentId);
                if (deptOpt.isPresent()) {
                    admin.setDepartment(deptOpt.get());
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Department not found"));
                }
            } else {
                admin.setDepartment(null);
            }

            Admin updated = adminRepository.save(admin);

            // Log admin update
            securityUtil.getCurrentUser()
                    .ifPresent(currentUser -> activityLogService.log(currentUser.getUsername(), "UPDATE_ADMIN",
                            "Updated admin: " + admin.getEmail(), null));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Department updated successfully");
            response.put("adminId", updated.getAdminId());
            if (updated.getDepartment() != null) {
                response.put("departmentId", updated.getDepartment().getDepartmentId());
                response.put("departmentName", updated.getDepartment().getName());
            } else {
                response.put("departmentId", null);
                response.put("departmentName", null);
            }

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update department: " + e.getMessage()));
        }
    }

    /**
     * Deactivate an admin (Super Admin only)
     */
    @PutMapping("/admins/{adminId}/deactivate")
    public ResponseEntity<?> deactivateAdmin(@PathVariable Long adminId) {
        try {
            securityUtil.requireSuperAdmin();

            if (adminId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin ID is required"));
            }
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();
            Optional<User> userOpt = userRepository.findByUsername(admin.getEmail()).stream().findFirst();
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setActive(false);
                userRepository.save(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found for this admin"));
            }

            Map<String, Object> response = Map.of(
                    "message", "Admin deactivated successfully",
                    "adminId", adminId,
                    "email", admin.getEmail(),
                    "active", false);

            // Broadcast real-time update
            webSocketService.broadcastAdminUpdate("DEACTIVATED", response);

            // Log deactivation
            securityUtil.getCurrentUser()
                    .ifPresent(currentUser -> activityLogService.log(currentUser.getUsername(), "DEACTIVATE_ADMIN",
                            "Deactivated admin: " + admin.getEmail(), null));

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate admin: " + e.getMessage()));
        }
    }

    /**
     * Activate an admin (Super Admin only)
     */
    @PutMapping("/admins/{adminId}/activate")
    public ResponseEntity<?> activateAdmin(@PathVariable Long adminId) {
        try {
            securityUtil.requireSuperAdmin();

            if (adminId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin ID is required"));
            }
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();
            Optional<User> userOpt = userRepository.findByUsername(admin.getEmail()).stream().findFirst();
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setActive(true);
                // Ensure user has ADMIN role
                if (user.getRole() != User.Role.ADMIN) {
                    user.setRole(User.Role.ADMIN);
                }
                userRepository.save(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found for this admin"));
            }

            Map<String, Object> response = Map.of(
                    "message", "Admin activated successfully",
                    "adminId", adminId,
                    "email", admin.getEmail(),
                    "active", true);

            // Broadcast real-time update
            webSocketService.broadcastAdminUpdate("ACTIVATED", response);

            // Log activation
            securityUtil.getCurrentUser()
                    .ifPresent(currentUser -> activityLogService.log(currentUser.getUsername(), "ACTIVATE_ADMIN",
                            "Activated admin: " + admin.getEmail(), null));

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to activate admin: " + e.getMessage()));
        }
    }

    /**
     * Delete an admin (Super Admin only)
     */
    @DeleteMapping("/admins/{adminId}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long adminId) {
        try {
            securityUtil.requireSuperAdmin();

            if (adminId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin ID is required"));
            }
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Admin not found"));
            }

            Admin admin = adminOpt.get();
            String adminEmail = admin.getEmail();
            String adminName = admin.getName();

            // Delete associated User if it exists
            Optional<User> userOpt = userRepository.findByUsername(adminEmail).stream().findFirst();
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userRepository.delete(user);
            }

            // Delete Admin
            adminRepository.delete(admin);

            Map<String, Object> response = Map.of(
                    "message", "Admin deleted successfully",
                    "adminId", adminId,
                    "name", adminName,
                    "email", adminEmail);

            // Broadcast real-time update
            webSocketService.broadcastAdminUpdate("DELETED", response);

            // Log deletion
            securityUtil.getCurrentUser()
                    .ifPresent(currentUser -> activityLogService.log(currentUser.getUsername(), "DELETE_ADMIN",
                            "Deleted admin: " + adminEmail, null));

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete admin: " + e.getMessage()));
        }
    }

    /**
     * Delete all admins (Super Admin only, protects Super Admin accounts)
     */
    @DeleteMapping("/admins/delete-all")
    public ResponseEntity<?> deleteAllAdmins() {
        try {
            securityUtil.requireSuperAdmin();

            List<Admin> allAdmins = adminRepository.findAll();
            int deletedCount = 0;

            for (Admin admin : allAdmins) {
                // Find associated User
                Optional<User> userOpt = userRepository.findByUsername(admin.getEmail()).stream().findFirst();

                // Protect Super Admin: skip deletion if the user is a Super Admin
                if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.SUPER_ADMIN) {
                    System.out.println("ℹ Skipping deletion of Super Admin: " + admin.getEmail());
                    continue;
                }

                // Delete associated User
                userOpt.ifPresent(userRepository::delete);

                // Delete Admin profile
                adminRepository.delete(admin);
                deletedCount++;
            }

            Map<String, Object> response = Map.of(
                    "message", "All admins deleted successfully (Super Admins preserved)",
                    "deletedCount", deletedCount);

            // Broadcast real-time update
            webSocketService.broadcastAdminUpdate("DELETED_ALL", response);

            // Log activity
            final int finalDeletedCount = deletedCount;
            securityUtil.getCurrentUser()
                    .ifPresent(currentUser -> activityLogService.log(currentUser.getUsername(), "DELETE_ALL_ADMINS",
                            "Deleted all admins (" + finalDeletedCount + " records removed)", null));

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete admins: " + e.getMessage()));
        }
    }
}
