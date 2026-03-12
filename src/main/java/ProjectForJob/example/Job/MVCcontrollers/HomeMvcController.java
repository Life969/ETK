package ProjectForJob.example.Job.MVCcontrollers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/", "/home"})
public class HomeMvcController {
    private static final Logger log = LoggerFactory.getLogger(HomeMvcController.class);

    @GetMapping
    public String home() {
        log.info("mapping home");
        return "home";
    }

}
