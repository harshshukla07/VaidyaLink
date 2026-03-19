package com.vaidyalink.backend.repository;

import com.vaidyalink.backend.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor,Long> {
    List<Doctor> findBySpeciality(String speciality);

    Optional<Doctor> findByEmail(String email);
}