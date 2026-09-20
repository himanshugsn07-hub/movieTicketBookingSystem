package com.himanshu.movieTicketBookingSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MovieTicketBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieTicketBookingSystemApplication.class, args);
	}

}
