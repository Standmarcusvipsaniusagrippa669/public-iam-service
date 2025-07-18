package com.valiantech.core.iam.clients;

import com.valiantech.core.iam.company.dto.CreateCompanyRequest;
import com.valiantech.core.iam.company.dto.UpdateCompanyRequest;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.company.dto.CompanyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyClient {

    private static final String BEARER = "Bearer ";
    private static final String COMPANY_NOT_FOUND = "Company not found";
    private static final String ERROR_CALLING_COMPANIES_SERVICE = "Error calling companies service: {}";
    private final WebClient.Builder webClientBuilder;

    @Value("${external-services.companies.base-url}")
    private final String baseUrl;
    @Value("${external-services.companies.base-path}")
    private final String companiesPath;
    public CompanyResponse findByRut(String rut, String jwt) {
        try {
            return webClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(companiesPath).queryParam("rut", rut).build())
                    .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            response -> Mono.error(new NotFoundException(COMPANY_NOT_FOUND)))
                    .bodyToMono(CompanyResponse.class)
                    .onErrorResume(NotFoundException.class, e -> Mono.empty()) // devuelve null
                    .block();
        } catch (WebClientResponseException e) {
            log.error(ERROR_CALLING_COMPANIES_SERVICE, e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    public CompanyResponse findById(UUID companyId, String jwt) {
        try {
            return webClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(companiesPath).pathSegment(companyId.toString()).build())
                    .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            response -> Mono.error(new NotFoundException(COMPANY_NOT_FOUND)))
                    .bodyToMono(CompanyResponse.class)
                    .onErrorResume(NotFoundException.class, e -> Mono.empty()) // devuelve null
                    .block();
        } catch (WebClientResponseException e) {
            log.error(ERROR_CALLING_COMPANIES_SERVICE, e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    public CompanyResponse findMeCompany(String jwt) {
        try {
            return webClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(companiesPath).pathSegment("me").build())
                    .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            response -> Mono.error(new NotFoundException(COMPANY_NOT_FOUND)))
                    .bodyToMono(CompanyResponse.class)
                    .onErrorResume(NotFoundException.class, e -> Mono.empty()) // devuelve null
                    .block(); // Sincrónico para tu flujo actual
        } catch (WebClientResponseException e) {
            log.error(ERROR_CALLING_COMPANIES_SERVICE, e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    public CompanyResponse createCompany(CreateCompanyRequest request, String jwt) {
        try {
            return webClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .uri(companiesPath)
                    .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 409,
                            response -> Mono.error(new ConflictException("Company already exists")))
                    .bodyToMono(CompanyResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error(ERROR_CALLING_COMPANIES_SERVICE, e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    public CompanyResponse update(UpdateCompanyRequest request, String jwt) {
        try {
            return webClientBuilder
                    .baseUrl(baseUrl)
                    .build()
                    .put()
                    .uri(uriBuilder -> uriBuilder.path(companiesPath).build())
                    .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            response -> Mono.error(new ConflictException("Company not found.")))
                    .bodyToMono(CompanyResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error(ERROR_CALLING_COMPANIES_SERVICE, e.getResponseBodyAsString(), e);
            throw e;
        }
    }
}
