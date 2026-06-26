package com.ems.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {
    @Test
    void success_shouldReturnCode200() {
        Result<String> result = Result.success("ok");
        assertEquals(200, result.getCode());
        assertEquals("ok", result.getData());
    }

    @Test
    void error_shouldReturnErrorMessage() {
        Result<Void> result = Result.error(500, "服务器错误");
        assertEquals(500, result.getCode());
        assertEquals("服务器错误", result.getMessage());
    }
}
