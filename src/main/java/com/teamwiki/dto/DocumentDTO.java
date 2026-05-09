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
public class DocumentDTO {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String visibility;
    private Boolean allowComment;
    private Long viewCount;
    private Long commentCount;
    private CategoryDTO category;
    private KnowledgeBaseDTO knowledgeBase;
    private UserDTO createdBy;
    private UserDTO lastEditedBy;
    private List<TagDTO> tags;
    private List<UserDTO> collaborators;
    private Boolean isFavorited;
    private String shareToken;
    private Boolean shareEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
