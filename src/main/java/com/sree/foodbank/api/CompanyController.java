package com.sree.foodbank.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sree.foodbank.model.*;
import com.sree.foodbank.service.CompanyService;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.json.simple.parser.ParseException;
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.base64.Base64Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.SecretKeySpec;
import javax.mail.MessagingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.sql.Array;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static javax.crypto.Cipher.SECRET_KEY;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequestMapping("/api/v1/company")
@RestController
public class CompanyController { //  Controller layer of the API
    private final CompanyService companyService;

    @Autowired
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping(path = "/key")
    public String getGKey() { //  This returns the google auth key for logging in. The server does this so the auth key is not stored on the front end where someone could see it.
        return "google-auth-key";
    }

    @PostMapping(path = "/gauthenticate")
    public boolean createGJwtAuthenticationToken(@RequestBody Token token, HttpServletRequest request, HttpServletResponse response, TimeZone timeZone) throws GeneralSecurityException, IOException, SQLException {
        companyService.checkGoogleAccount(token);
        Cookie cookie = new Cookie("token", token.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie); //  Issues an HTTPOnly cookie to the user
        return true;
    }

    @GetMapping(path = "/gauthenticate")
    public boolean deleteGJwtAuthenticationToken(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie); //  Deletes the user's HTTPOnly cookie
        return true;
    }

    @PostMapping(path = "/gaccount")
    public boolean createGAccount(@RequestBody @Valid @NonNull CreateAccount createAccount) throws SQLException {
        return companyService.createGAccount(createAccount);
    }

    @GetMapping(path = "/authenticate")
    public boolean createJwtAuthenticationToken(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie); //  Issues an HTTPOnly cookie to the user
        return true;
    }

    @GetMapping(path = "/dashboard")
    public List<CompanyInfo> dashboard(@CookieValue("token") String token, CompanyDashboard companyDashboard) throws GeneralSecurityException, IOException {
        return companyService.dashboard(token, companyDashboard);
    }

    @PostMapping(path = "/login")
    public boolean login(@RequestBody Login login, HttpServletResponse response) throws SQLException {
        String token = companyService.login(login);
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie); //  Issues an HTTPOnly cookie to the user
        return true;
    }

    @GetMapping(path = "/food")
    public List<Food> getAllFood() {
        return companyService.getAllFood();
    }

    @PostMapping(path = "/filter")
    public List<CompanyReturn> filterFood(@Valid @NonNull @RequestBody CompanyFilter companyFilter) throws SQLException {
        return companyService.filterFood(companyFilter);
    }

    @PostMapping(path = "/request")
    public void requestFood(@CookieValue(name = "token") String token, @Valid @NonNull @RequestBody CompanyRequest companyRequest) throws GeneralSecurityException, IOException, SQLException, MessagingException {
        companyService.requestFood(token, companyRequest);
    }

    @GetMapping
    public List<CompanyReturn> getAllCompanies(CompanyReturn companyReturn) throws SQLException {
        return companyService.getAllCompanies(companyReturn);
    }

    @GetMapping(path = "{id}")
    public List<CompanyReturn> getCompanyById(@PathVariable("id") BigDecimal id) {
        return companyService.getCompanybyId(id, 0.0);
    }

    @GetMapping(path = "{id}/needed")
    public List<CompanyNeededFood> getCompanyNeededFoodById(@PathVariable("id") BigDecimal id) {
        return companyService.getCompanyNeededFoodById(id);
    }

    @PostMapping(path = "/request/get")
    public List<CompanyReturnRequest> getRequest(@CookieValue(name = "token") String token) throws SQLException, GeneralSecurityException, IOException {
        return companyService.getRequest(token);
    }

    @DeleteMapping(path = "{id}")
    public void deleteCompany(@CookieValue(name = "token") String token) throws GeneralSecurityException, IOException, SQLException {
        companyService.deleteCompany(token);
    }

    @PutMapping(path = "/")
    public void updateCompany(@CookieValue(name = "token") String token, @Valid @NonNull @RequestBody CompanyInfo companyUpdate) throws GeneralSecurityException, IOException, SQLException, InterruptedException {
        companyService.updateCompany(token, companyUpdate);
    }

    @PutMapping(path = "/food")
    public void updateFood(@CookieValue(name = "token") String token, @Valid @NonNull @RequestBody CompanyUpdateFood foodUpdate) throws GeneralSecurityException, IOException, SQLException {
        companyService.updateFood(token, foodUpdate);
    }

    @PutMapping(path = "/request")
    public int updateRequest(@Valid @NonNull @RequestBody CompanyRequest companyRequest) throws GeneralSecurityException, SQLException, IOException {
        return companyService.updateRequest(companyRequest);
    }

    @PostMapping(path = "/create")
    public boolean createAccount(@Valid @NonNull @RequestBody CreateAccount createAccount) throws SQLException {
        return companyService.createAccount(createAccount);
    }

    @PostMapping(path = "/location")
    public List<CompanyReturn> locationFiltering(@Valid @NonNull @RequestBody Location location) throws SQLException, IOException, InterruptedException {
        return companyService.locationFiltering(location);
    }

    @PostMapping(path = "/bothfilter")
    public List<CompanyReturn> bothFilter(@Valid @NonNull @RequestBody CompanyBothFilter companyBothFilter) throws InterruptedException, SQLException, IOException {
        return companyService.bothFilter(companyBothFilter);
    }

}
