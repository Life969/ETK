package ProjectForJob.example.Job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JobApplication {

	public static void main(String[] args) {
		System.setProperty("org.apache.tomcat.util.http.fileupload.FileUploadBase.maxFileCount", "10");
		SpringApplication.run(JobApplication.class, args);
	}

}
