package com.teamwiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbRequest {
    private String name;
    private String description;
    private String visibility;
    private String department;
    private String icon;
}
