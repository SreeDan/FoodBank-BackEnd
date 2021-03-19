package com.sree.foodbank.service;

import com.sree.foodbank.dao.CompanyDao;
import com.sree.foodbank.model.*;
import org.apache.http.conn.params.ConnManagerPNames;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.text.Bidi;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyService { //  Service layer of the API
    private final CompanyDao companyDao;

    @Autowired
    public CompanyService(@Qualifier("postgres") CompanyDao companyDao) {
        this.companyDao = companyDao;
    }

    public List<CompanyInfo> dashboard(String token, CompanyDashboard companyDashboard) throws GeneralSecurityException, IOException {
        return companyDao.dashboard(token, companyDashboard);
    }

    public String login(Login login) throws SQLException {
        return companyDao.login(login);
    }

    public List<Food> getAllFood() {
        return companyDao.getAllFood();
    }

    public List<CompanyReturn> filterFood(CompanyFilter companyFilter) throws SQLException {
        return companyDao.filterByFood(companyFilter);
    }

    public List<CompanyReturn> getAllCompanies(CompanyReturn companyReturn) throws SQLException {
        return companyDao.selectAllCompanies(companyReturn);
    }

    public List<CompanyReturn> getCompanybyId(BigDecimal id, Double distance) {
        return companyDao.selectCompanyById(id, distance);
    }

    public List<CompanyNeededFood> getCompanyNeededFoodById(BigDecimal id) {
        return companyDao.getCompanyNeededFoodById(id);
    }

    public int deleteCompany(String token) throws GeneralSecurityException, IOException, SQLException {
        return companyDao.deleteCompanybyId(token);
    }

    public int updateCompany(String token, CompanyInfo companyUpdate) throws GeneralSecurityException, IOException, SQLException, InterruptedException {
        return companyDao.updateCompanybyId(token, companyUpdate);
    }

    public int updateFood(String token, CompanyUpdateFood foodUpdate) throws GeneralSecurityException, IOException, SQLException {
        return companyDao.updateFood(token, foodUpdate);
    }

    public int requestFood(String token, CompanyRequest companyRequest) throws GeneralSecurityException, SQLException, IOException, MessagingException {
        return companyDao.requestFood(token, companyRequest);
    }

    public List<CompanyReturnRequest> getRequest(String token) throws SQLException, GeneralSecurityException, IOException {
        return companyDao.getRequest(token);
    }

    public int updateRequest(CompanyRequest companyRequest) throws SQLException, GeneralSecurityException, IOException {
        return companyDao.updateRequest(companyRequest);
    }

    public void sendEmail() throws IOException, ParseException {
        companyDao.sendEmail(new String[0], "");
    }

    public boolean createAccount(CreateAccount createAccount) throws SQLException {
        return companyDao.createAccount(createAccount);
    }

    public List<CompanyReturn> locationFiltering(Location location) throws SQLException, IOException, InterruptedException {
        return companyDao.locationFiltering(location);
    }

    public List<CompanyReturn> bothFilter(CompanyBothFilter companyBothFilter) throws InterruptedException, SQLException, IOException {
        return companyDao.bothFilter(companyBothFilter);
    }

    public void checkGoogleAccount(Token token) throws GeneralSecurityException, SQLException, IOException {
        companyDao.checkGoogleAccount(token);
    }

    public boolean createGAccount(CreateAccount createAccount) throws SQLException {
        return companyDao.createGAccount(createAccount);
    }
}
