package com.example.turf_Backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Slf4j
public class CustomAsyncExceptionHandler  implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Async error in method {} with params {}: {}",
                method.getName(), params, ex.getMessage());
    }
}
