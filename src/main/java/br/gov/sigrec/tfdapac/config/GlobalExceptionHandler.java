package br.gov.sigrec.tfdapac.config;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception ex, HttpServletRequest request, Principal principal) {
        Throwable root = rootCause(ex);
        String usuario = principal == null ? "ANONIMO" : principal.getName();

        log.error("Erro interno SIGREc | metodo={} | uri={} | usuario={} | erro={} | causa={}",
                request.getMethod(),
                request.getRequestURI(),
                usuario,
                ex.getMessage(),
                root.getMessage(),
                ex);

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", 500);
        mav.addObject("message", "Internal Server Error");
        mav.addObject("erroDetalhado", root.getMessage());
        mav.addObject("rota", request.getMethod() + " " + request.getRequestURI());
        return mav;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }
        return result;
    }
}
