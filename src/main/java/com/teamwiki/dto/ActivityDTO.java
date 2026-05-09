package com.teamwiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDTO {
    private Long id;
    private UserDTO user;
    private Long documentId;
    private String documentTitle;
    private String type;
    private String description;
    private LocalDateTime createdAt;
}
