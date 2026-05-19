package com.example.CarService.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.crypto.dsig.spec.XPathType;
import java.io.IOException;

@Component
public class ApiKeyFilter implements Filter {

    @Value("${api.key}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {


        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;


        String path = httpRequest.getRequestURI();

        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3") ||
                path.startsWith("/h2-console")) {
            chain.doFilter(request, response);
            return;
        }

        String key = httpRequest.getHeader("X-API-Key");


        if (apiKey.equals(key)) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(401);
            httpResponse.getWriter().write("Unauthorized");
        }
    }
}
