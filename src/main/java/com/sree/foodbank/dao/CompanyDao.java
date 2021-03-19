package com.sree.foodbank.dao;

import com.sree.foodbank.model.*;
import org.json.simple.parser.ParseException;

import javax.mail.MessagingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;

public interface CompanyDao {

    List<CompanyInfo> dashboard(String token, CompanyDashboard companyDashboard) throws GeneralSecurityException, IOException; //  Gives the data for the fashboard

    String login(Login login) throws SQLException; //  Logs in the user and gives the user a token to authenticate themselves with the server

    List<Food> getAllFood(); //  Returns all food in the database

    List<CompanyReturn> selectAllCompanies(CompanyReturn companyReturn) throws SQLException; //  Returns a list of all the food banks

    List<CompanyFood> selectCompanyAvailable(String token) throws GeneralSecurityException, IOException;

    List<CompanyFood> selectCompanyNeeded(String token) throws GeneralSecurityException, IOException;

    List<CompanyReturn> filterByFood(CompanyFilter companyFilter) throws SQLException; // Returns a list of food banks based on food being filtered

    List<CompanyReturn> selectCompanyById(BigDecimal id, Double distance); //  Returns a food bank based on the id

    List<CompanyNeededFood> getCompanyNeededFoodById(BigDecimal id); //  Returns food that a food bank needs

    int deleteCompanybyId(String token) throws GeneralSecurityException, IOException, SQLException;

    int updateCompanybyId(String token, CompanyInfo companyInfo) throws GeneralSecurityException, IOException, SQLException, InterruptedException; //  Updates the information of a company

    int updateFood(String token, CompanyUpdateFood foodUpdate) throws GeneralSecurityException, IOException, SQLException; //  Updates a food bank's food

    int requestFood(String token, CompanyRequest companyRequest) throws SQLException, IOException, GeneralSecurityException, MessagingException; //  Creates a request to a food bank from another user

    int updateRequest(CompanyRequest companyRequest) throws SQLException, GeneralSecurityException, IOException; // Updates a request

    List<CompanyReturnRequest> getRequest (String token) throws SQLException, GeneralSecurityException, IOException; //  Returns all requests for a user

    String foodIdToName(BigDecimal foodId) throws SQLException; //  Converts food ids to food names

    BigDecimal foodNameToId(String foodName) throws SQLException; //  Converts food names to food ids

    BigDecimal googleToken(String token) throws GeneralSecurityException, IOException; //  Decodes a Google issued token for authentication

    void sendEmail(String[] Info, String date) throws IOException, ParseException; //  Sends an email to the user that the request has been confirmed

    boolean createAccount(CreateAccount createAccount) throws SQLException; //  Creates an account

    List<CompanyReturn> locationFiltering(Location location) throws SQLException, IOException, InterruptedException; //  Returns food banks based on a user's location and sorts the food banks from closest to farthest

    List<CompanyReturn> bothFilter(CompanyBothFilter companyBothFilter) throws SQLException, IOException, InterruptedException;

    void checkGoogleAccount(Token token) throws SQLException, GeneralSecurityException, IOException; //  Checks if a Google user is logging in for the first time

    boolean createGAccount(CreateAccount createAccount) throws SQLException; //  If a Google user is logging in for the first time, this adds the Google account to the records.
}
