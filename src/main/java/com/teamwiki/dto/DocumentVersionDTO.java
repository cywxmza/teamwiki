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
public class DocumentVersionDTO {
    private Long id;
    private Integer versionNumber;
    private String changeDescription;
    private UserDTO createdBy;
    private LocalDateTime createdAt;
}
