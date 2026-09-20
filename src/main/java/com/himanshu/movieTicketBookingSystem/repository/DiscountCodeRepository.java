package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.constants.Queries;
import com.himanshu.movieTicketBookingSystem.entity.DiscountCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Integer> {

    // Finds a code by its upper-case text.
    Optional<DiscountCode> findByCode(String code);

    // Finds a code under a write lock so redemptions are counted without races.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(Queries.DiscountCode.BY_CODE)
    Optional<DiscountCode> findByCodeForUpdate(@Param("code") String code);
}
