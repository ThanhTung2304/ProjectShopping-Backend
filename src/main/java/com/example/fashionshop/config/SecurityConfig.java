/**
 * quan trọng nhất
 * cấu hình security
 * mở endpoint public
 * chặn endpoint private
 * add JWT filter
 */

package com.example.fashionshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Cho phép dùng @PreAuthorize trên method
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)  // REST API không cần CSRF
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Không dùng session
                .authorizeHttpRequests(auth -> auth

                        // ===== PUBLIC =====
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/test").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                                .requestMatchers("/api/payments/vnpay/ipn").permitAll()
                                .requestMatchers("/api/payments/vnpay/return").permitAll()
                                .requestMatchers("/api/chat/**").permitAll()

                        // Xem sản phẩm, danh mục, đánh giá không cần đăng nhập
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/coupons/**").permitAll()

                                // ===== ADMIN =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        // ===== CUSTOMER =====
//                        .requestMatchers("/api/cart/**").hasRole("CUSTOMER")
//                        .requestMatchers("/api/orders/**").authenticated()
//                        .requestMatchers("/api/addresses/**").authenticated()
//                        .requestMatchers("/api/reviews/**").authenticated()
//                        .requestMatchers("/api/users/**").authenticated()


                        .requestMatchers("/api/cart/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers("/api/orders/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers("/api/me/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers("/api/payments/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers("/api/addresses/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers("/api/reviews/**")
                        .authenticated()

                        .requestMatchers("/api/users/**")
                        .authenticated()

                        .anyRequest().authenticated()
                )
                // Thêm JwtAuthFilter trước filter mặc định của Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {return config.getAuthenticationManager();
    }
}
