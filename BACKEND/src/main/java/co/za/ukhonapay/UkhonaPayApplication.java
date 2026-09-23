package co.za.ukhonapay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UkhonaPayApplication {
    public static void main(String[] args) {
        SpringApplication.run(UkhonaPayApplication.class, args);
    }
}
