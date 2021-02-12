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
public class CompanyController {
    private final CompanyService companyService;

    @Autowired
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping(path = "/email/test")
    public void sendEmail() throws IOException, ParseException {
        companyService.sendEmail();
    }

    @GetMapping(path = "/jwt")
    public void decodeJWT() throws UnsupportedEncodingException {
        Instant now = Instant.now();
        byte[] secret = Base64.getDecoder().decode("decode");
        String jwt = Jwts.builder()
                .setSubject("sub")
                .setAudience("Audience")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor(secret))
                .compact();
        System.out.println(jwt);

    }

    @PostMapping(path = "/gauthenticate")
    public boolean createGJwtAuthenticationToken(@RequestBody TokenTest tokenTest, HttpServletRequest request, HttpServletResponse response, TimeZone timeZone) {
        //Cookie cookie = new Cookie("gtoken", tokenTest.getToken());
        Cookie cookie = new Cookie("token", tokenTest.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return true;
    }

    @GetMapping(path = "/gauthenticate")
    public boolean deleteGJwtAuthenticationToken(HttpServletResponse response) {
        //Cookie cookie = new Cookie("gtoken", null);
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return true;
    }

    @GetMapping(path = "/authenticate")
    public boolean createJwtAuthenticationToken(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return true;
    }

    @GetMapping(path = "/tokens")
    public boolean setTokens(HttpServletResponse response) {
        Cookie sCookie = new Cookie("token", null);
        sCookie.setHttpOnly(true);
        sCookie.setPath("/");
        response.addCookie(sCookie);
        /*Cookie gCookie = new Cookie("gtoken", null);
        gCookie.setHttpOnly(true);
        gCookie.setPath("/");
        response.addCookie(gCookie);*/
        return true;
    }

    @PostMapping(path = "/dashboard/access")
    public boolean accessDashboard(@CookieValue(name = "signedIn") Boolean signedIn, @CookieValue(name = "gsignedIn") Boolean gsignedIn) {
        return signedIn || gsignedIn;
    }

    @PostMapping(path = "/dashboard")
    public List<CompanyReturn> dashboard(@CookieValue("token") String token, CompanyDashboard companyDashboard) throws GeneralSecurityException, IOException {
        return companyService.dashboard(token, companyDashboard);
    }

    @PostMapping(path = "/readauthenticate")
    public boolean readToken(@CookieValue(name = "token") String token, @RequestBody TokenBody tokenBody) {
        System.out.println("The token is " + token);
        System.out.println(tokenBody.getTest());
        return true;
    }

    @PostMapping(path = "/login")
    public boolean login(@RequestBody Login login, HttpServletResponse response, TimeZone timeZone) throws SQLException {
        String token = companyService.login(login);
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return true;
    }

    @GetMapping(path="/login")
    public boolean testLogin(HttpServletResponse response) {
        Cookie cookie = new Cookie("ntoken", "token");
        response.addCookie(cookie);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return true;
    }

    @PostMapping
    public CompanyReturn addCompany(@Valid @NonNull String token, @Valid @NonNull @RequestBody Company company) throws GeneralSecurityException, IOException {
        return companyService.addCompany(token, company);
    }

    @GetMapping(path = "/food")
    public List<Food> getAllFood() {
        return companyService.getAllFood();
    }

    @PostMapping(path = "/companyfood")
    public void addFood(@Valid @NonNull String token, @Valid @NonNull @RequestBody CompanyFood companyFood) throws GeneralSecurityException, IOException, SQLException {
        companyService.addFood(token, companyFood);
    }

    @PostMapping(path = "/filter")
    public List<CompanyReturn> filterFood(@Valid @NonNull @RequestBody CompanyFilter companyFilter) throws SQLException {
        return companyService.filterFood(companyFilter);
    }

    @PostMapping(path = "/request")
    public void requestFood(@CookieValue(name = "token") String token, @Valid @NonNull @RequestBody CompanyRequest companyRequest) throws GeneralSecurityException, IOException, SQLException, MessagingException {
        //g
        companyService.requestFood(token, companyRequest);
    }

    @GetMapping("/all-cookies")
    public String readAllCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .map(c -> c.getName() + "=" + c.getValue()).collect(Collectors.joining(", "));
        }
        return "No cookies";
    }

    @GetMapping
    public List<CompanyReturn> getAllCompanies(CompanyReturn companyReturn) throws SQLException {
        return companyService.getAllCompanies(companyReturn);
    }

    @GetMapping(path = "/companyfood")
    public List<CompanyReturnFood> getCompanyFood() {
        return companyService.getCompanyFood();
    }

    @GetMapping(path = "{id}")
    public List<CompanyReturn> getCompanyById(@PathVariable("id") BigDecimal id) {
        return companyService.getCompanybyId(id);
    }

    @GetMapping(path = "{id}/needed")
    public List<CompanyNeededFood> getCompanyNeededFoodById(@PathVariable("id") BigDecimal id) {
        return companyService.getCompanyNeededFoodById(id);
    }

    @PostMapping(path = "/request/get")
    public List<CompanyReturnRequest> getRequest(@CookieValue(name = "token") String token) throws SQLException, GeneralSecurityException, IOException {
        //g
        return companyService.getRequest(token);
    }

    @PutMapping(path = "/image")
    public int addImage(@Valid @NonNull @RequestBody ImageEncode imageEncode) throws FileNotFoundException, SQLException {
        return companyService.addImage(imageEncode);
    }


    @DeleteMapping(path = "{id}")
    public void deleteCompany(@PathVariable("id") BigDecimal id) {
        companyService.deleteCompany(id);
    }

    @DeleteMapping(path = "/food")
    public void deleteFood(BigDecimal id, @Valid @NonNull String token) throws GeneralSecurityException, IOException {
        companyService.deleteFood(id, token);
    }

    @PutMapping(path = "/")
    public void updateCompany(@CookieValue(name = "token") String token, @Valid @NonNull @RequestBody CompanyInfo companyUpdate) throws GeneralSecurityException, IOException, SQLException, InterruptedException {
        companyService.updateCompany(token, companyUpdate);
    }

    @PutMapping(path = "/food")
    public void updateFood(@CookieValue(name = "token") String token, @Valid @NonNull @RequestBody List<Food> foodUpdate) throws GeneralSecurityException, IOException, SQLException {
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

    @PostMapping(path="/location")
    public CompanyReturn locationFiltering(Location location) {
        return companyService.locationFiltering(location);
    }

}
