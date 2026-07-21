package com.skylark.skylarkbiagentbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables {@code @CreatedDate}/{@code @LastModifiedDate} auto-population on Mongo
 * documents (conversations, reports, cache entries). MongoDB here is memory/cache
 * only, never the business database — see ADR-001 "MongoDB Usage".
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
