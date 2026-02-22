package com.vaidyalink.backend.repository;

import com.vaidyalink.backend.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor,Long> {
    List<Doctor> findBySpeciality(String speciality);
}
