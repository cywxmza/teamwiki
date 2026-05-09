package com.teamwiki.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    @NotBlank(message = "评论内容不能为空")
    private String content;
    private Long parentId;
    private String selectedText;
    private Boolean isInline;
}
