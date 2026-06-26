package com.ems.security.context;

import lombok.Data;
import lombok.Builder;
import java.util.Set;

@Data
@Builder
public class CurrentUser {
    private Long userId;
    private String username;
    private String name;
    private Long deptId;
    private Set<String> permissions;
    private Integer dataScope;
}
