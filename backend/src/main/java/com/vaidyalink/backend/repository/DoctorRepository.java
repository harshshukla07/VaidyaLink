package com.vaidyalink.backend.repository;

import com.vaidyalink.backend.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor,Long> {
    List<Doctor> findBySpeciality(String speciality);

    Optional<Doctor> findByEmail(String email);

    @Query("SELECT DISTINCT d.speciality FROM Doctor d ORDER BY d.speciality")
    List<String> findDistinctSpecialities();
}