package com.isaqcasey.aidocsassistant.Security;

import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.TokenBlacklistService;
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
    private final TokenBlacklistService tokenBlacklistService;

    public JWTFilter(JWTService jwt, TokenBlacklistService tokenBlacklistService)
    {
        this.jwt = jwt;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException
    {
        String authHeader = request.getHeader("Authorization");
        System.out.println("Filter triggered for URL: " + request.getRequestURI());
        System.out.println("Auth Header: " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer "))
        {
            String token = authHeader.substring(7);
            System.out.println("Extracted Token: " + token);

            try
            {
                // Check if token is blacklisted (invalidated during logout)
                if (tokenBlacklistService.isTokenInvalidated(token)) {
                    System.out.println("Token is blacklisted (invalidated)");
                    // Don't set authentication - token is invalid
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = jwt.extractUserName(token);
                System.out.println("Extracted Username: " + username);

                // Validate token is not expired
                if (jwt.isTokenExpired(token)) {
                    System.out.println("Token has expired");
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                null
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authentication set in SecurityContext for: " + username);

            }
            catch (Exception e)
            {
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

