package com.teamwiki.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    private String content;
    private String summary;
    private Long categoryId;
    private Long knowledgeBaseId;
    private List<Long> tagIds;
    private String visibility;
    private Boolean allowComment;
}
