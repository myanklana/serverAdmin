package adminServer.mvp.security;

import adminServer.mvp.auth.JwtService;
import adminServer.mvp.user.User;
import adminServer.mvp.user.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) { this.jwtService = jwtService; this.users = users; }

    @Override protected void doFilterInternal( @NonNull HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                User user = users.findById(jwtService.userId(authorization.substring(7))).orElseThrow();
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(new AuthenticatedUser(user.getId(), user.getUsername()), null, List.of()));
            } catch (RuntimeException e) {
                throw new ServletException("Invalid JWT token", e);

             }
        }
        chain.doFilter(request, response);
    }
}
