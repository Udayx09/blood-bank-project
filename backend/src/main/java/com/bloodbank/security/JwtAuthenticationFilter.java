package com.bloodbank.security;

import com.bloodbank.entity.BloodBank;
import com.bloodbank.entity.Donor;
import com.bloodbank.repository.BloodBankRepository;
import com.bloodbank.repository.DonorRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final BloodBankRepository bloodBankRepository;
    private final DonorRepository donorRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
            BloodBankRepository bloodBankRepository,
            DonorRepository donorRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.bloodBankRepository = bloodBankRepository;
        this.donorRepository = donorRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                // Check if it's a donor token or bank token
                if (jwtTokenProvider.isDonorToken(token)) {
                    // Handle donor authentication
                    Long donorId = jwtTokenProvider.getDonorIdFromToken(token);
                    Donor donor = donorRepository.findById(donorId).orElse(null);

                    if (donor != null) {
                        DonorPrincipal principal = new DonorPrincipal(
                                donor.getId(),
                                donor.getName(),
                                donor.getPhone(),
                                donor.getBloodType(),
                                donor.getCity());

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                Collections.emptyList());

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } else {
                    // Handle bank authentication
                    Long bankId = jwtTokenProvider.getBankIdFromToken(token);
                    BloodBank bank = bloodBankRepository.findById(bankId).orElse(null);

                    if (bank != null) {
                        BankPrincipal principal = new BankPrincipal(
                                bank.getId(),
                                bank.getName(),
                                bank.getPhone(),
                                bank.getCity());

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                Collections.emptyList());

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
