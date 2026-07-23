package com.mca.automate;

import com.mca.automate.dto.VerifyOtpDTO;
import com.mca.automate.service.ChromeBrowserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/* JADX INFO: loaded from: McaAutomationApplication.class */
@SpringBootApplication
public class McaAutomationApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(McaAutomationApplication.class, args);
        
        // If run locally with a cookie argument, inject it into local Chrome and exit
        String localCookie = System.getProperty("local.mca.cookie");
        if (localCookie != null && !localCookie.isBlank()) {
            ChromeBrowserService chromeService = context.getBean(ChromeBrowserService.class);
            VerifyOtpDTO dto = new VerifyOtpDTO();
            dto.setDeviceId(System.getProperty("local.mca.deviceId", ""));
            System.out.println("\n=======================================================");
            System.out.println("Starting local Chrome injection via CDP...");
            boolean success = chromeService.openWithCookies(localCookie, dto);
            if (success) {
                System.out.println("Successfully opened Chrome with session cookies!");
            } else {
                System.out.println("Failed to open Chrome. Check the logs above.");
            }
            System.out.println("=======================================================\n");
            System.exit(0);
        }
    }
}
