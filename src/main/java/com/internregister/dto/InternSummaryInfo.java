package com.internregister.dto;

import com.internregister.entity.Department;
import com.internregister.entity.Location;
import com.internregister.entity.Supervisor;
import java.time.LocalDate;

public interface InternSummaryInfo {
    Long getInternId();
    String getName();
    String getEmail();
    DepartmentInfo getDepartment();
    SupervisorInfo getSupervisor();
    LocationInfo getLocation();
    String getField();
    String getEmployer();
    String getIdNumber();
    LocalDate getStartDate();
    LocalDate getEndDate();
    Boolean getActive();
    Boolean getIsProfileComplete();
    
    interface DepartmentInfo {
        Long getDepartmentId();
        String getName();
    }
    
    interface SupervisorInfo {
        Long getId();
        String getName();
    }
    
    interface LocationInfo {
        Long getLocationId();
        String getName();
    }
}
