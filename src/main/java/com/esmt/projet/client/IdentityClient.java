package com.esmt.projet.client;

import com.esmt.projet.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "identity-service", path = "/api/users", configuration = FeignConfig.class)
public interface IdentityClient {

    @GetMapping("/verify/{id}/{requiredRole}")
    boolean verifyUserRole(@PathVariable("id") Long id, @PathVariable("requiredRole") String requiredRole);

    @GetMapping("/id/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id);



}
