package com.internregister.repository;

import com.internregister.entity.InternContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternContractRepository extends JpaRepository<InternContract, Long> {
    Optional<InternContract> findByIntern_InternId(Long internId);
    
    // ✅ Batch query: get all intern IDs that have contracts — avoids N+1 queries
    @Query("SELECT ic.intern.internId FROM InternContract ic WHERE ic.intern.internId IN :ids")
    List<Long> findInternIdsWithContracts(List<Long> ids);
}
