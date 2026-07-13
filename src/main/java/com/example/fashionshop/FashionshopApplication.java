package com.example.fashionshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class FashionshopApplication {
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx =
				SpringApplication.run(FashionshopApplication.class, args);

		// Debug: liệt kê tất cả bean
		String[] beans = ctx.getBeanDefinitionNames();
		for (String bean : beans) {
			if (bean.toLowerCase().contains("controller") ||
					bean.toLowerCase().contains("user") ||
					bean.toLowerCase().contains("product")) {
				System.out.println("BEAN: " + bean);
			}
		}
	}
}