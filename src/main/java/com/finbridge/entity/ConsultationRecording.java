package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "consultation_recordings")
public class ConsultationRecording {
    @Id
    @Column(name = "consultation_id")
    private UUID consultationId;

    @Column(name = "recording_enabled")
    private Boolean recordingEnabled = false;

    @Column(name = "video_url")
    private String videoUrl = "";
}
