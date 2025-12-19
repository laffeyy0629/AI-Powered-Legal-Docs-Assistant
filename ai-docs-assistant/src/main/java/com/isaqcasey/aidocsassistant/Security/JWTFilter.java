package com.isaqcasey.aidocsassistant.Security;

import com.isaqcasey.aidocsassistant.Service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTFilter extends OncePerRequestFilter
{
    private final JWTService jwt;

    public JWTFilter(JWTService jwt)
    {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException
    {
        String authHeader = request.getHeader("Authorization");
        // 1. Check if the filter is even being triggered and what the header looks like
        System.out.println("Filter triggered for URL: " + request.getRequestURI());
        System.out.println("Auth Header: " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer "))
        {
            String token = authHeader.substring(7);
            // 2. Check if the token was sliced correctly
            System.out.println("Extracted Token: " + token);

            try
            {
                String username = jwt.extractUserName(token);
                // 3. Check if the JWT utility successfully parsed the username
                System.out.println("Extracted Username: " + username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                null // 4. POTENTIAL ISSUE HERE (See below)
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authentication set in SecurityContext for: " + username);

            }
            catch (Exception e)
            {
                // 5. Catch any parsing or expiration errors
                System.out.println("JWT Extraction failed: " + e.getMessage());
            }
        }
        else
        {
            System.out.println("No Bearer token found in header.");
        }

        filterChain.doFilter(request, response);
    }
}