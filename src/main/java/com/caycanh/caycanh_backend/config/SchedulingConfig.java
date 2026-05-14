package com.caycanh.caycanh_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật chế độ scheduled task cho toàn bộ ứng dụng.
 * Chỉ cần class trống có annotation — Spring tự kích hoạt.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
