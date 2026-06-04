package com.zhukovskiy.platform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex,
                                         RedirectAttributes redirectAttributes) {
        log.warn("Resource not found: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/greet";
    }

    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRule(BusinessRuleException ex,
                                     RedirectAttributes redirectAttributes) {
        log.warn("Business rule violation: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/greet";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     RedirectAttributes redirectAttributes) {
        log.warn("Access denied: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/greet";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Static resource not found: {}", ex.getMessage());
        return "redirect:/greet";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex,
                                         RedirectAttributes redirectAttributes) {
        log.error("Unexpected error", ex);
        redirectAttributes.addFlashAttribute("error", "Произошла непредвиденная ошибка");
        return "redirect:/greet";
    }
}