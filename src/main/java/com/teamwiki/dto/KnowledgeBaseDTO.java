package com.teamwiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDTO {
    private Long id;
    private String name;
    private String description;
    private String visibility;
    private UserDTO owner;
    private String department;
    private String icon;
    private Long documentCount;
    private List<UserDTO> members;
    private Boolean isOwner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
