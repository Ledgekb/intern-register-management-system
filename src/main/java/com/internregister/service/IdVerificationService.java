package com.internregister.service;

import com.internregister.repository.InternRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class IdVerificationService {

    @Autowired
    private InternRepository internRepository;

    public Map<String, Object> verifySouthAfricanId(String idNumber) {
        Map<String, Object> result = new HashMap<>();

        if (idNumber == null || idNumber.trim().length() != 13 || !idNumber.matches("^\\d{13}$")) {
            result.put("valid", false);
            result.put("message", "ID number must consist of exactly 13 numeric digits.");
            return result;
        }

        String cleanId = idNumber.trim();

        // 1. Check database uniqueness
        if (internRepository.existsByIdNumber(cleanId)) {
            result.put("valid", false);
            result.put("message", "This ID number is already registered in the system.");
            return result;
        }

        // 2. Validate Luhn Checksum Algorithm
        if (!isValidLuhn(cleanId)) {
            result.put("valid", false);
            result.put("message", "Invalid South African ID number checksum.");
            return result;
        }

        // 3. Extract Birth Date (YYMMDD)
        int year2Digit = Integer.parseInt(cleanId.substring(0, 2));
        int month = Integer.parseInt(cleanId.substring(2, 4));
        int day = Integer.parseInt(cleanId.substring(4, 6));

        int currentYear = LocalDate.now().getYear();
        int current2DigitYear = currentYear % 100;
        int fullYear = (year2Digit <= current2DigitYear) ? (2000 + year2Digit) : (1900 + year2Digit);

        try {
            LocalDate birthDate = LocalDate.of(fullYear, month, day);
            if (birthDate.isAfter(LocalDate.now())) {
                result.put("valid", false);
                result.put("message", "Invalid date of birth encoded in ID number.");
                return result;
            }
            result.put("birthDate", birthDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "Invalid birth date encoded in ID number.");
            return result;
        }

        // 4. Extract Gender (Digits 7-10: 0000-4999 = Female, 5000-9999 = Male)
        int genderDigits = Integer.parseInt(cleanId.substring(6, 10));
        String gender = (genderDigits >= 5000) ? "Male" : "Female";
        result.put("gender", gender);

        // 5. Extract Citizenship (Digit 11: 0 = SA Citizen, 1 = Permanent Resident)
        int citizenshipDigit = Integer.parseInt(cleanId.substring(10, 11));
        String citizenship = (citizenshipDigit == 0) ? "South African Citizen" : "Permanent Resident";
        result.put("citizenship", citizenship);

        result.put("valid", true);
        result.put("message", "ID Verified Successfully (" + citizenship + " | " + gender + ")");
        return result;
    }

    private boolean isValidLuhn(String id) {
        int sum = 0;
        boolean alternate = false;
        for (int i = id.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(id.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
