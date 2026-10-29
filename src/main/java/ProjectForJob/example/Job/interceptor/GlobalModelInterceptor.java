package ProjectForJob.example.Job.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class GlobalModelInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        // Добавляем currentUri в модель, только если она существует
        if (modelAndView != null) {
            String requestURI = request.getRequestURI();
            modelAndView.addObject("currentUri", requestURI);
        }
    }
}
