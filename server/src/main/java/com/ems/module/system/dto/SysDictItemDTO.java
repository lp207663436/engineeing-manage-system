package com.ems.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysDictItemDTO {
    private Long id;
    @NotNull(message = "字典ID不能为空")
    private Long dictId;
    @NotBlank(message = "字典项标签不能为空")
    private String label;
    @NotBlank(message = "字典项值不能为空")
    private String value;
    private Integer sort;
}
