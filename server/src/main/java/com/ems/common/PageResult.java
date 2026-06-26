package com.ems.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;

    public static <T> PageResult<T> of(List<T> list, long total) {
        PageResult<T> p = new PageResult<>();
        p.setList(list);
        p.setTotal(total);
        return p;
    }
}
