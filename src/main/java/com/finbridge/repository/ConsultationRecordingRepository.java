package com.finbridge.repository;

import com.finbridge.entity.ConsultationRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ConsultationRecordingRepository extends JpaRepository<ConsultationRecording, UUID> {
}
