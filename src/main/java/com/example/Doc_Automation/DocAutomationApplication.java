package com.example.Doc_Automation;

import com.example.Doc_Automation.controller.WatchdogController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import java.io.File;

@SpringBootApplication
public class DocAutomationApplication {
	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(DocAutomationApplication.class, args);
		System.out.println("---  System started  ---");


		String[] dirs = {"input", "exports", "archive", "errors"};
		for (String d : dirs) {
			File dir = new File(d);
			if (!dir.exists()) dir.mkdirs();
		}

		WatchdogController watchdog = context.getBean(WatchdogController.class);
		try {
			watchdog.startWatching("input");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}