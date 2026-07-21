package com.skylark.skylarkbiagentbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SkylarkBiAgentBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkylarkBiAgentBackendApplication.class, args);
	}

}
