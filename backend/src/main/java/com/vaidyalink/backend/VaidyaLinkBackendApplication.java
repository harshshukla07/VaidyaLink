package com.vaidyalink.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VaidyaLinkBackendApplication {

	public static void main(String[] args) {
        SpringApplication.run(VaidyaLinkBackendApplication.class, args);


	}

}
