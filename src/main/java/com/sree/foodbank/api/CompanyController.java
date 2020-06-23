package com.sree.foodbank.api;

import com.sree.foodbank.model.Company;
import com.sree.foodbank.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/api/v1/company")
@RestController
public class CompanyController {
    private final CompanyService companyService;

    @Autowired
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public void addCompany(@Valid @NonNull @RequestBody Company company) {
        companyService.addCompany(company);
    }

    @GetMapping
    public List<Company> getallCompanies() {
        return companyService.getAllCompanies();
    }

    @GetMapping(path = "{id}")
    public Company getCompanyById(@PathVariable("id") UUID id) {
        return companyService.getCompanybyId((id))
                .orElse(null);
    }

    @DeleteMapping(path = "{id}")
    public void deleteCompany(@PathVariable("id") UUID id) {
        companyService.deleteCompany(id);
    }

    @PutMapping(path = "{id}")
    public void updateCompany(@PathVariable("id") UUID id, @Valid @NonNull @RequestBody Company companyUpdate) {
        companyService.updateCompany(id, companyUpdate);
    }

    @GetMapping(path = "user")
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        return Collections.singletonMap("email", principal.getAttribute("email"));
    }
}
