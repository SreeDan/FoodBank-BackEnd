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

    List<CompanyReturn> dashboard(String token, CompanyDashboard companyDashboard) throws GeneralSecurityException, IOException;

    String login(Login login) throws SQLException;

    List<Food> getAllFood();

    CompanyReturn insertCompany(BigDecimal id, String token, Company company) throws GeneralSecurityException, IOException;

    default CompanyReturn insertCompany(String token, Company company) throws GeneralSecurityException, IOException {
        BigDecimal id = new BigDecimal("0");
        return insertCompany(id, token, company);
    }

    int insertFood(BigDecimal id, String token, CompanyFood companyFood) throws GeneralSecurityException, IOException;

    default int insertFood(String token, CompanyFood companyFood) throws GeneralSecurityException, IOException, SQLException {
        BigDecimal id = new BigDecimal("0");
        return insertFood(id, token, companyFood);
    }

    List<CompanyReturn> selectAllCompanies(CompanyReturn companyReturn) throws SQLException;

    List<CompanyReturnFood> selectCompanyFood();

    List<CompanyReturn> filterByFood(CompanyFilter companyFilter) throws SQLException;

    List<CompanyReturn> selectCompanyById(BigDecimal id);

    List<CompanyNeededFood> getCompanyNeededFoodById(BigDecimal id);

    int deleteCompanybyId(BigDecimal id);

    int deleteFood(BigDecimal id, String token) throws GeneralSecurityException, IOException;

    int updateCompanybyId(BigDecimal id, String token, Company company) throws GeneralSecurityException, IOException, SQLException;

    int updateFood(String token, CompanyFood companyFood) throws GeneralSecurityException, IOException, SQLException;

    int requestFood(String token, CompanyRequest companyRequest) throws SQLException, IOException, GeneralSecurityException, MessagingException;

    int updateRequest(CompanyRequest companyRequest) throws SQLException, GeneralSecurityException, IOException;

    List<CompanyReturnRequest> getRequest (String token) throws SQLException, GeneralSecurityException, IOException;


    /*default int updateFood(CompanyFood companyFood) throws GeneralSecurityException, IOException {
        BigDecimal id = new BigDecimal("0");
        return updateFood(id, companyFood);
    }

     */
    /*int encode(ImageEncode base64) throws FileNotFoundException, SQLException;

    List<ImageEncode> getEncode(ImageEncode imageEncode) throws FileNotFoundException, SQLException;

     */

    int addImage(ImageEncode imageEncode) throws FileNotFoundException, SQLException;

    String foodIdToName(BigDecimal foodId) throws SQLException;

    BigDecimal foodNameToId(String foodName) throws SQLException;

    BigDecimal googleToken(String token) throws GeneralSecurityException, IOException;

    void sendEmail(String[] Info, String date) throws IOException, ParseException;

    boolean createAccount(CreateAccount createAccount) throws SQLException;

    //List<CompanyReturn> test(Test test) throws ParseException, SQLException;
}
