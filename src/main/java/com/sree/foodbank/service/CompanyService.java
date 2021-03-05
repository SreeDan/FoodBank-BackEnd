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
public class CompanyService {
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

    public CompanyReturn addCompany(String token, Company company) throws GeneralSecurityException, IOException {
        return companyDao.insertCompany(token, company);
    }

    public int addFood(String token, CompanyFood companyFood) throws GeneralSecurityException, IOException, SQLException {
        return companyDao.insertFood(token, companyFood);
    }

    public List<CompanyReturn> filterFood(CompanyFilter companyFilter) throws SQLException {
        return companyDao.filterByFood(companyFilter);
    }

    public List<CompanyReturn> getAllCompanies(CompanyReturn companyReturn) throws SQLException {
        return companyDao.selectAllCompanies(companyReturn);
    }

    public List<CompanyReturnFood> getCompanyFood() {
        return companyDao.selectCompanyFood();
    }

    public List<CompanyReturn> getCompanybyId(BigDecimal id, Double distance) {
        return companyDao.selectCompanyById(id, distance);
    }

    public List<CompanyNeededFood> getCompanyNeededFoodById(BigDecimal id) {
        return companyDao.getCompanyNeededFoodById(id);
    }

    public int deleteCompany(BigDecimal id) {
        return companyDao.deleteCompanybyId(id);
    }

    public int updateCompany(String token, CompanyInfo companyUpdate) throws GeneralSecurityException, IOException, SQLException, InterruptedException {
        return companyDao.updateCompanybyId(token, companyUpdate);
    }

    public int updateFood(String token, List<Food> foodUpdate) throws GeneralSecurityException, IOException, SQLException {
        return companyDao.updateFood(token, foodUpdate);
    }

    public int deleteFood(BigDecimal id, String token) throws GeneralSecurityException, IOException {
        return companyDao.deleteFood(id, token);
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

    public int addImage(ImageEncode imageEncode) throws FileNotFoundException, SQLException {
        return companyDao.addImage(imageEncode);
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

    public void checkGoogleAccount(Token token) throws GeneralSecurityException, SQLException, IOException {
        companyDao.checkGoogleAccount(token);
    }

    public boolean createGAccount(CreateAccount createAccount) throws SQLException {
        return companyDao.createGAccount(createAccount);
    }
}
